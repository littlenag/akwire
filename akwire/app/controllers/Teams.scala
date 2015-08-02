package controllers

import models.User
import scaldi.{Injector, Injectable}
import services.CoreServices

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Logger
import play.api.libs.json.Reads._
import play.api.libs.json._

import securesocial.core.{RuntimeEnvironment, SecureSocial}

import org.bson.types.ObjectId

import com.mongodb.casbah.commons.Imports._

import scala.util.{Try, Failure, Success}

class Teams(implicit inj: Injector, override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] with Injectable {

  val core = inject[CoreServices]

  import models.Team
  import models.RuleConfig

  def createTeam = SecuredAction.async(parse.json) {
    implicit request =>

      val customReads: Reads[String] = (__ \ "name").read(minLength[String](3))

      customReads.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { name =>
          Team.insert(Team(name))
          Future.successful(Created(s"Team Created"))
        }
      )
  }

  def retrieveTeams = SecuredAction.async {
    Future {
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = Team.find(filter).sort(sort).toList
      Logger.info(s"teams: $list")
      Ok(Json.toJson(list))
    }
  }

  def retrieveTeam(teamId:String) = SecuredAction.async {
    Future {
      Try {
        new ObjectId(teamId)
      }.map { id =>
        Team.findOne(MongoDBObject("_id" -> id)) match {
          case Some(team : Team) => Ok(Json.toJson(team))
          case None => BadRequest(s"Invalid id $teamId")
        }
      }.recover {
        case e => BadRequest(s"Invalid id $teamId")
      }.get
    }
  }

  def renameTeam(teamId:String, oldName:String, newName:String) = SecuredAction.async {
    Future {
      Team.findOne(MongoDBObject("_id" -> new ObjectId(teamId))) match {
        case Some(team : Team) =>
          if (team.name == oldName) {
            Team.save(team.copy(name = newName))
            Ok(s"Team name changed from '$oldName' to '$newName'")
          } else {
            Conflict(s"Team name already updated, now has name '${team.name}', expected '$oldName'")
          }
        case None => BadRequest(s"Invalid id $teamId")
      }
    }
  }

  def deleteTeam(teamId:String) = SecuredAction.async {
    Future {
      Team.removeById(new ObjectId(teamId))
      Ok(s"Removed team with id $teamId")
    }
  }

  // ------------------------------------------ //
  // API Methods for dealing with Rules         //
  // ------------------------------------------ //

  def createRule(teamId:String) = SecuredAction.async(parse.json[RuleConfig]) { request =>
    Future {
      Logger.info(s"Saving rule for team: ${teamId}")

      // TODO check user access to team

      val rule = request.body.copy(teamId = new ObjectId(teamId))

      Logger.info(s"Saving rule: ${rule}")

      core.createRule(rule) match {
        case Success(v) => Ok(Json.toJson(v))
        case Failure(e) => BadRequest(s"${e.getMessage}")
      }
    }
  }

  def updateRule(teamId:String) = SecuredAction.async(parse.json[RuleConfig]) { request =>
    Future {
      Logger.info(s"Updating rule for team: ${teamId}")

      // TODO check user access to team

      val rule = request.body.copy(teamId = new ObjectId(teamId))

      Logger.info(s"Updating rule: ${rule}")

      core.updateRule(rule) match {
        case Success(v) => Ok(Json.toJson(v))
        case Failure(e) => BadRequest(s"${e.getMessage}")
      }
    }
  }

  def deleteRule(teamId:String, ruleId:String) = SecuredAction.async {
    Future {
      Ok("placeholder")
    }
  }

  def startRule(teamId:String, ruleId:String) = SecuredAction.async {
    Future {
      Ok("placeholder")
    }
  }

  def pauseRule(teamId:String, ruleId:String) = SecuredAction.async {
    Future {
      Ok("placeholder")
    }
  }

}
