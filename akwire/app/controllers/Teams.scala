package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import javax.inject.Named
import play.api.mvc._
import play.api.data._
import play.api.libs.json.Reads._
import play.api.libs.json._
import com.novus.salat.dao.SalatDAO

import org.bson.types.ObjectId
import org.joda.time.DateTime

import play.api.Play.current
import play.api.PlayException

import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.{MongoConnection}

import models.mongoContext._

import com.mongodb.casbah.MongoConnection

class Teams extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Teams])

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models.Team

  object TeamsDAO extends SalatDAO[Team, ObjectId](MongoConnection()("akwire")("teams"))

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
          TeamsDAO.insert(team)
          Future.successful(Created(s"Team Created"))
        }
      )
  }

  def retrieveTeams = Action.async {
    Future {
      //val filter = MongoDBObject.empty
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = TeamsDAO.find(filter).sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveTeam(teamId:String) = Action.async {
    Future {
      val filter = MongoDBObject("_id" -> new ObjectId(teamId))
      TeamsDAO.findOne(filter) match {
        case Some(team : Team) => Ok(Json.toJson(team))
        case None => BadRequest(s"Invalid id $teamId")
      }
    }
  }

  def updateTeam(teamId:String) = Action.async(parse.json) {
    request =>
      request.body.asOpt[Team] match {
        case Some(team: Team) =>
          TeamsDAO.save(team)
          Future.successful(Ok(Json.toJson(team)))
        case None =>
          Future.successful(BadRequest(s"Could not parse team with id $teamId"))
      }
  }

  def deleteTeam(teamId:String) = Action.async {
    Future {
      TeamsDAO.removeById(new ObjectId(teamId))
      Ok(s"Removed team with id $teamId")
    }
  }
}
