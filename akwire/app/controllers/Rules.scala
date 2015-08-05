package controllers

import models.mongoContext._
import models._
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json
import scaldi.{Injector, Injectable}
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import services.CoreServices

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class Rules(implicit inj: Injector, override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] with Injectable {

  val core = inject[CoreServices]

  // PathBindable is used to handle map complex URL segments more gracefully

  def createRule(entity:OwningEntity) = SecuredAction.async(parse.json[RuleConfig]) { request =>
    Future {
      Logger.info(s"Saving rule for entity: $entity")

      // TODO check user access to entity (team,user,service)

      val rule = request.body.copy(id = ObjectId.get())

      implicit val scope = entity

      Logger.info(s"Saving rule: $rule")

      core.saveRule(rule) match {
        case Success(v) => Ok(Json.toJson(v))
        case Failure(e) => BadRequest(s"${e.getMessage}")
      }
    }
  }

  def updateRule(entity:OwningEntity) = SecuredAction.async(parse.json[RuleConfig]) { request =>
    Future {
      Logger.info(s"Updating rule for team: $entity")

      // TODO check user access to team

      val rule = request.body

      Logger.info(s"Updating rule: $rule")

      core.saveRule(rule) match {
        case Success(v) => Ok(Json.toJson(v))
        case Failure(e) => BadRequest(s"${e.getMessage}")
      }
    }
  }

  def deleteRule(entity:OwningEntity, ruleId:String) = SecuredAction.async {
    Future {
      Ok("placeholder")
    }
  }

  def startRule(entity:OwningEntity, ruleId:String) = SecuredAction.async {
    Future {
      Ok("placeholder")
    }
  }

  def pauseRule(entity:OwningEntity, ruleId:String) = SecuredAction.async {
    Future {
      Ok("placeholder")
    }
  }
}
