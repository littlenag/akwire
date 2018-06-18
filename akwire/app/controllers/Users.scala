package controllers

import com.mongodb.casbah.commons.MongoDBObject
import models.{ContactInfo, User}
import org.bson.types.ObjectId
import scaldi.{Injectable, Injector}
import securesocial.core.{AuthenticationMethod, BasicProfile, RuntimeEnvironment, SecureSocial}
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils.PasswordHasher

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, AnyContent}

/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 */
class Users(implicit inj: Injector, val env: RuntimeEnvironment[User]) extends SecureSocial[User] with ConstraintReads with Injectable {

  val hasher = inject[PasswordHasher]

  val provider = UsernamePasswordProvider.UsernamePassword

  val readUserCreateRequest: Reads[(String, String, String)] = (
    (__ \ "email").read[String](email keepAnd min(5)) and
    (__ \ "name").read[String] and
    (__ \ "password").read[String]
  ) tupled

  def createUser: Action[JsValue] = SecuredAction.async(parse.json) { implicit request =>
    readUserCreateRequest.reads(request.body).fold(
      invalid = { errors => Future.successful(BadRequest("invalid json")) },
      valid = { case (email, name, password) =>

        val pw = Some(hasher.hash(password))
        val id = ObjectId.get()
        val profile = BasicProfile(provider, email, None, None, Some(name), Some(email), None, AuthenticationMethod.UserPassword, None, None, pw)

        // TODO Users should always be a member of their own private eponymous Team and other teams
        // as an admin decides
        val user = new User(id, profile, ContactInfo(None), Nil)
        User.insert(user)
        Future.successful(Created(s"User Created"))
      }
    )
  }

  def retrieveUsers: Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      val sort = MongoDBObject("profile.userId" -> 1)
      val list = User.findAll().sort(sort).toList
      Ok(Json.toJson(list))
    }
  }

  def retrieveUserById(userId:String): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      User.findOne(MongoDBObject("_id" -> new ObjectId(userId))) match {
        case Some(user: User) => Ok(Json.toJson(user))
        case None => BadRequest(s"Invalid id $userId")
      }
    }
  }

  def retrieveUserByEmail(email:String): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      User.findOne(MongoDBObject("profile.email" -> email)) match {
        case Some(user: User) => Ok(Json.toJson(user))
        case None => BadRequest(s"Invalid user email $email")
      }
    }
  }

  def deleteUser(userId:String): Action[AnyContent] = SecuredAction.async { implicit request =>
    Future {
      User.removeById(new ObjectId(userId))
      Ok(s"Removed user with id $userId")
    }
  }
}
