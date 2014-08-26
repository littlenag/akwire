package controllers

import scaldi.{Injector, Injectable}
import services.CoreServices

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import play.api.libs.json.Reads._
import play.api.libs.json._

import org.bson.types.ObjectId
import org.joda.time.DateTime

import com.mongodb.casbah.commons.Imports._

import scala.util.{Failure, Success}

class Teams(implicit inj: Injector) extends Controller with Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Teams])

  val core = inject[CoreServices]

  import models.Team
  import models.Rule

  def createTeam = Action.async(parse.json) {
    request =>

      // minLength(3) tupled
      //val customReads: Reads[(String, String)] = (__ \ "name").read[String] and (__ \ "foo").read[String] tupled
      val customReads: Reads[String] = (__ \ "name").read(minLength[String](3))

      customReads.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { res =>
          val name: String = res
          val team = new Team(ObjectId.get(), name, Nil, new DateTime(), true)
          Team.insert(team)
          Future.successful(Created(s"Team Created"))
        }
      )
  }

  def retrieveTeams = Action.async {
    Future {
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = Team.find(filter).sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveTeam(teamId:String) = Action.async {
    Future {
      Team.findOne(MongoDBObject("_id" -> new ObjectId(teamId))) match {
        case Some(team : Team) => Ok(Json.toJson(team))
        case None => BadRequest(s"Invalid id $teamId")
      }
    }
  }

  def renameTeam(teamId:String, oldName:String, newName:String) = Action.async {
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

  def deleteTeam(teamId:String) = Action.async {
    Future {
      Team.removeById(new ObjectId(teamId))
      Ok(s"Removed team with id $teamId")
    }
  }

  // ------------------------------------------ //
  // API Methods for dealing with Rules         //
  // ------------------------------------------ //

  // ruleId in body means update existing rule
  def saveRule(teamId:String) = Action.async(parse.json) { request =>
    Future {
      request.body.asOpt[Rule] match {
        case Some(rule: Rule) =>
          core.saveRule(new ObjectId(teamId), rule) match {
            case Success(v) => Ok(Json.toJson(v))
            case Failure(e) => BadRequest(s"${e.getMessage}")
          }
        case None => BadRequest(s"Could not parse request body")
      }

    }
  }

  def deleteRule(teamId:String, ruleId:String) = Action.async {
    Future {
      Ok("placeholder")
    }
  }

  def startRule(teamId:String, ruleId:String) = Action.async {
    Future {
      Ok("placeholder")
    }
  }

  def pauseRule(teamId:String, ruleId:String) = Action.async {
    Future {
      Ok("placeholder")
    }
  }

}
