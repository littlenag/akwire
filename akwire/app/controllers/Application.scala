package controllers

import models.{User, Team}
import play.api.libs.json._
import scaldi.{Injector, Injectable}
import securesocial.controllers.ProviderController
import services.{UUIDGenerator}
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._

import scala.concurrent.Future

/**
 * Instead of declaring an object of Application as per the template project, we must declare a class given that
 * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
 * @param inj IuuidGenerator the UUID generator service we wish to receive.
 */
class Application(implicit inj: Injector) extends Controller with ConstraintReads with Injectable {

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
/*
  def authenticate(provider:String) = Action.async { implicit request =>
    if (request.remoteAddress == "127.0.0.1") {
      logger.warn("Accepting login from localhost")
      User.findByEmailAndProvider(User.AKWIRE_ADMIN_ACCT_EMAIL, User.AKWIRE_ADMIN_PROVIDER) match {
        case Some(user) =>
          val sr = ProviderController.completeAuthentication(user, session)
          Future.successful(sr.withHeaders(("ADMIN_ACCOUNT_NAME", User.AKWIRE_ADMIN_ACCT_EMAIL)))
        case None =>
          Future.successful(InternalServerError("No admin account found"))
      }
    } else {
      logger.info("Try login from: " + request.remoteAddress)
      ProviderController.authenticate(provider)(request)
    }
  }
*/
}
