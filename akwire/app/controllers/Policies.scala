package controllers

import models.{OwningEntityRef, Policy, User}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import scaldi.{Injectable, Injector}
import securesocial.core.{RuntimeEnvironment, SecureSocial}

import scala.concurrent.Future

/**
 * The Policies controller encapsulates the REST endpoints for working with Notification Policies.
 */
class Policies(implicit inj: Injector, implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] with ConstraintReads with Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Teams])

  import play.api.libs.json._

  def createPolicy(): Action[JsValue] = SecuredAction.async(parse.json) { implicit request =>
    Future {
      request.body.validate[Policy].map { policy =>
        val newPolicy = policy.copy(id = ObjectId.get())
        Policy.save(policy)
        Created(Json.toJson(newPolicy))
      }.recoverTotal {
        errors => BadRequest("invalid json")
      }
    }
  }

  def updatePolicy(): Action[Policy] = SecuredAction.async(parse.json[Policy]) { implicit request =>
    Future {
      // TODO check user access to policy
      logger.info(s"Updating policy for ${request.body.owner}")
      val policy = request.body
      Policy.save(policy)
      logger.info(s"Saving policy: $policy")
      Ok(Json.toJson(policy))
    }
  }

  def retrievePolicies(): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      Ok(Json.toJson(Policy.findAll().toList))
    }
  }

  def retrievePolicyById(policyId:String): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      Policy.findOneById(new ObjectId(policyId)) match {
        case Some(policy : Policy) => Ok(Json.toJson(policy))
        case None => BadRequest(s"Invalid id $policyId")
      }
    }
  }

  def deletePolicy(policyId:String): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      Policy.removeById(new ObjectId(policyId))
      Ok(s"Removed policy with id $policyId")
    }
  }

  // Non-CRUD methods

  def retrieveDefaultPolicy(owner:OwningEntityRef): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      Policy.findDefaultForOwner(owner) match {
        case Some(policy : Policy) => Ok(Json.toJson(policy))
        case None => BadRequest(s"No default user policy for user ${request.user}")
      }
    }
  }

}
