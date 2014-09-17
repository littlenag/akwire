package controllers

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatDAO
import models.User
import org.bson.types.ObjectId

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
class Users extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Users])

  import models.User

  def createUser = Action.async(parse.json) {
    request =>

      // minLength(3) tupled
      //val customReads: Reads[(String, String)] = (__ \ "name").read[String] and (__ \ "foo").read[String] tupled
      val customReads: Reads[String] = (__ \ "name").read(minLength[String](3))

      customReads.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { res =>
          val name: String = res
          val user = new User(ObjectId.get(), name, Nil, new DateTime(), true)
          User.insert(user)
          Future.successful(Created(s"User Created"))
        }
      )
  }

  def retrieveUsers = Action.async {
    Future {
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = User.find(filter).sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveUser(userId:String) = Action.async {
    Future {
      User.findOne(MongoDBObject("_id" -> new ObjectId(userId))) match {
        case Some(user : User) => Ok(Json.toJson(user))
        case None => BadRequest(s"Invalid id $userId")
      }
    }
  }

  def renameUser(userId:String, oldName:String, newName:String) = Action.async {
    Future {
      User.findOne(MongoDBObject("_id" -> new ObjectId(userId))) match {
        case Some(user : User) =>
          if (user.name == oldName) {
            User.save(user.copy(name = newName))
            Ok(s"User name changed from '$oldName' to '$newName'")
          } else {
            Conflict(s"User name already updated, now has name '${user.name}', expected '$oldName'")
          }
        case None => BadRequest(s"Invalid id $userId")
      }
    }
  }

  def deleteUser(userId:String) = Action.async {
    Future {
      User.removeById(new ObjectId(userId))
      Ok(s"Removed user with id $userId")
    }
  }


}
