package controllers

import play.api.libs.json.{Reads, ConstraintReads}
import scaldi.{Injector, Injectable}
import securesocial.controllers.ProviderController
import securesocial.core._
import services.{UUIDGenerator}
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

import models.User

import ExecutionContext.Implicits.global

/**
 * Instead of declaring an object of Application as per the template project, we must declare a class given that
 * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
 * @param inj IuuidGenerator the UUID generator service we wish to receive.
 */
class Application(implicit inj: Injector) extends Controller with ConstraintReads with Injectable {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])

  val uuidGenerator = inject[UUIDGenerator]

  def index = Action {
    logger.info("Serving index page...")
    Ok(views.html.index())
  }

  def randomUUID = Action {
    logger.info("calling UUIDGenerator...")
    Ok(uuidGenerator.generate.toString)
  }

  def handleAuth(provider: String) = Action { implicit request =>
    Registry.providers.get(provider) match {
      case Some(p) => {
        try {
          p.authenticate().fold( result => result , {
            user => user match {
              case user:User =>
                val sr = ProviderController.completeAuthentication(user, session)
                User.findByEmailAndProvider(user.mail, provider) match {
                  case Some(user) =>
                    logger.info("authenticated and found user!")
                    Ok(Json.toJson(user.copy(pwdInfo = None)))
                  case None =>
                    logger.info("authenticated but could not find user!")
                    InternalServerError("Could not find User record after authentication")
                }
              case _ =>
                BadRequest(Json.obj("errors" -> Json.obj("message" -> "expected a User object")))
            }
          })
        } catch {
          case ex: AccessDeniedException => {
            BadRequest(Json.obj("errors" -> Json.obj("message" -> "invalid credentials")))
          }

          case other: Throwable => {
            logger.error("Unable to log user in. An exception was thrown", other)
            InternalServerError("Error encountered while logging in")
          }
        }
      }
      case _ => NotFound
    }
  }
}
