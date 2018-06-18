package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import play.api.libs.json.Reads._
import play.api.libs.json._
import com.novus.salat.dao.SalatDAO

import org.bson.types.ObjectId
import org.joda.time.DateTime

import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.MongoClient

import models.mongoContext._

/**
 * @see
 */
class Roles extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Roles])

  import models._
  import models.Role.roleFormatter

  object RolesDAO extends SalatDAO[Role, ObjectId](MongoClient()("akwire")("roles"))

  val readRoleCreateRequest: Reads[String] = (__ \ "name").read(minLength[String](3))

  def createRole: Action[JsValue] = Action.async(parse.json) {
    request =>

      readRoleCreateRequest.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { res =>
          val name: String = res
          val role = new Role(ObjectId.get(), name, new DateTime(), true)
          RolesDAO.insert(role)
          Future.successful(Created(s"Role Created"))
        }
      )
  }

  def retrieveRoles: Action[AnyContent] = Action.async {
    Future {
      //val filter = MongoDBObject.empty
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = RolesDAO.find(filter).sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveRole(roleId:String): Action[AnyContent] = Action.async {
    Future {
      val filter = MongoDBObject("_id" -> new ObjectId(roleId))
      RolesDAO.findOne(filter) match {
        case Some(role : Role) => Ok(Json.toJson(role))
        case None => BadRequest(s"Invalid id $roleId")
      }
    }
  }

  def updateRole(roleId:String): Action[JsValue] = Action.async(parse.json) {
    request =>
      request.body.asOpt[Role] match {
        case Some(role: Role) =>
          RolesDAO.save(role)
          Future.successful(Ok(Json.toJson(role)))
        case None =>
          Future.successful(BadRequest(s"Could not parse role with id $roleId"))
      }
  }

  def deleteRole(roleId:String): Action[AnyContent] = Action.async {
    Future {
      RolesDAO.removeById(new ObjectId(roleId))
      Ok(s"Removed role with id $roleId")
    }
  }
}
