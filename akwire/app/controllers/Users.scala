package controllers

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatDAO
import models.User
import org.bson.types.ObjectId
import org.joda.time.DateTime
import securesocial.core.{SecureSocial, PasswordInfo}
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils.BCryptPasswordHasher

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import play.api.libs.json._

/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 */
class Users extends Controller with ConstraintReads with SecureSocial {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Users])

  import models.User

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  def nukeUser = SecuredAction(ajaxCall = true) { implicit request =>
    Ok("")
  }

  def nukeUser2 = SecuredAction(ajaxCall = true).async(parse.json) {
    implicit request =>
    Future {
      Ok("")
    }
  }

  def createUser = SecuredAction(ajaxCall = true).async(parse.json) {
    implicit request =>

      // minLength(3) tupled
      //val customReads: Reads[(String, String)] = (__ \ "name").read[String] and (__ \ "foo").read[String] tupled
      val customReads: Reads[(String, String, String)] = ((__ \ "email").read[String](email keepAnd min(5)) and (__ \ "name").read[String] and (__ \ "password").read[String]) tupled

      customReads.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { res =>
          val email = res._1
          val name = res._2
          val password = res._3

          val pw = (new BCryptPasswordHasher(play.api.Play.current)).hash(password)

          val provider = UsernamePasswordProvider.UsernamePassword

          // TODO User's should always be a member of their own private eponymous Team and other teams
          // as an admin decides
          val user = new User(ObjectId.get(), email, provider, name, Some(pw), Nil)
          User.insert(user)
          Future.successful(Created(s"User Created"))
        }
      )
  }

  def retrieveUsers = SecuredAction(ajaxCall = true).async {
    implicit request =>
    Future {
      val sort = MongoDBObject("name" -> 1)
      val list = User.findAll().sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveUserById(userId:String) = SecuredAction(ajaxCall = true).async {
    implicit request =>
    Future {
      User.findOne(MongoDBObject("_id" -> new ObjectId(userId))) match {
        case Some(user : User) => Ok(Json.toJson(user))
        case None => BadRequest(s"Invalid id $userId")
      }
    }
  }

  def retrieveUserByEmail(email:String) = SecuredAction(ajaxCall = true).async {
    implicit request =>
      Future {
        User.findOne(MongoDBObject("mail" -> email)) match {
          case Some(user : User) => Ok(Json.toJson(user))
          case None => BadRequest(s"Invalid user email $email")
        }
      }
  }

  def deleteUser(userId:String) = SecuredAction(ajaxCall = true).async {
    implicit request =>
    Future {
      User.removeById(new ObjectId(userId))
      Ok(s"Removed user with id $userId")
    }
  }
}
