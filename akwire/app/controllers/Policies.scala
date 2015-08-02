package controllers

import com.mongodb.casbah.commons.MongoDBObject
import models.{Policy, User}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import scaldi.{Injectable, Injector}
import securesocial.core.{RuntimeEnvironment, SecureSocial}

import scala.concurrent.Future

/**
 * The Policies controller encapsulates the REST endpoints for working with Notification Policies.
 */
class Policies(implicit inj: Injector, implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] with ConstraintReads with Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Teams])

  import play.api.libs.json._

  def createUserPolicy() = SecuredAction.async(parse.json) {
    implicit request =>
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

  def updateUserPolicy() = SecuredAction.async(parse.json[Policy]) { implicit request =>
    logger.info(s"Updating policy for user")

    // TODO check user access to policy

    Future {
      val policy = request.body
      Policy.save(policy)
      logger.info(s"Saving policy: ${policy}")
      Ok(Json.toJson(policy))
    }
  }

  def retrieveUserPolicies() = SecuredAction.async {
    implicit request =>
      Future {
        val list = User.findAll().toList
        Ok(Json.arr(list)(0))
      }
  }

  def retrievePolicyById(policyId:String) = SecuredAction.async {
    implicit request =>
      Future {
        Policy.findOne(MongoDBObject("_id" -> new ObjectId(policyId))) match {
          case Some(policy : Policy) => Ok(Json.toJson(policy))
          case None => BadRequest(s"Invalid id $policyId")
        }
      }
  }

  def deletePolicy(policyId:String) = SecuredAction.async {
    implicit request =>
      Future {
        Policy.removeById(new ObjectId(policyId))
        Ok(s"Removed policy with id $policyId")
      }
  }

  // Non-CRUD methods

  def retrieveDefaultUserPolicy = SecuredAction.async {
    implicit request =>
      Future {
        Policy.findOne(MongoDBObject("userId" -> request.user.id, "default" -> true)) match {
          case Some(policy : Policy) => Ok(Json.toJson(policy))
          case None => BadRequest(s"No default user policy for user ${request.user}")
        }
      }
  }

}