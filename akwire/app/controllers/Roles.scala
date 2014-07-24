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

//import play.api.libs.functional.syntax._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import play.api.Play.current
import play.api.PlayException

import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.{MongoConnection}

import com.novus.salat.Context

import controllers.mongoContext._

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoConnection

/**
 * @see
 */
@Named
class Roles extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Roles])

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models._
  import models.Role.roleFormatter

  object RolesDAO extends SalatDAO[Role, ObjectId](MongoConnection()("akwire")("roles"))

  def createRole = Action.async(parse.json) {
    request =>
      /*
       * request.body is a JsValue.
       * There is an implicit Writes that turns this JsValue as a JsObject,
       * so you can call insert() with this JsValue.
       * (insert() takes a JsObject as parameter, or anything that can be
       * turned into a JsObject using a Writes.)
       *
       * http://www.playframework.com/documentation/2.2.2/ScalaJsonCombinators
       */

      // minLength(3) tupled
      //val customReads: Reads[(String, String)] = (__ \ "name").read[String] and (__ \ "foo").read[String] tupled
      val customReads: Reads[String] = (__ \ "name").read(minLength[String](3))

      customReads.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { res =>
          val name: String = res
          val role = new Role(ObjectId.get(), name, new DateTime(), true)
          RolesDAO.insert(role)
          Future.successful(Created(s"Role Created"))
        }
      )
  }

  def retrieveRoles = Action.async {
    Future {
      //val filter = MongoDBObject.empty
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = RolesDAO.find(filter).sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveRole(roleId:String) = Action.async {
    Future {
      val filter = MongoDBObject("_id" -> new ObjectId(roleId))
      RolesDAO.findOne(filter) match {
        case Some(role : Role) => Ok(Json.toJson(role))
        case None => BadRequest(s"Invalid id $roleId")
      }
    }
  }

  def updateRole(roleId:String) = Action.async(parse.json) {
    request =>
      request.body.asOpt[Role] match {
        case Some(role: Role) =>
          RolesDAO.save(role)
          Future.successful(Ok(Json.toJson(role)))
        case None =>
          Future.successful(BadRequest(s"Could not parse role with id $roleId"))
      }
  }

  def deleteRole(roleId:String) = Action.async {
    Future {
      RolesDAO.removeById(new ObjectId(roleId))
      Ok(s"Removed role with id $roleId")
    }
  }
}
