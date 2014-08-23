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

  import models.mongoContext._

  object UsersDAO extends SalatDAO[User, ObjectId](MongoConnection()("akwire")("users"))


  def createUser = Action.async(parse.json) {
    request =>
      request.body.validate[User].map {
        user =>
          UsersDAO.insert(user)
          logger.debug(s"Successfully inserted")
          Future.successful(Created(s"User Created"))
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findUsers = Action.async {
    Future {
      Ok(Json.arr(UsersDAO.find(MongoDBObject("active"->true)).sort(orderBy = MongoDBObject("created" -> -1)).toList))
    }
  }

}
