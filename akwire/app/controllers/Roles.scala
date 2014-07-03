package controllers

import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import javax.inject.Named
import play.api.mvc._
import play.api.libs.json.Reads._
import play.api.libs.json._
//import play.api.libs.functional.syntax._
import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * @see
 */
@Named
class Roles extends Controller with MongoController {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Roles])

  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("roles")

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models._

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
          collection.insert(role).map {
            lastError =>
              logger.debug(s"Successfully create Role with LastError: $lastError")
              Created(s"Role Created")
          }
        }
      )
  }

  def findAllRoles = Action.async {
    // let's do our query
    val cursor: Cursor[Role] = collection.
      // find all
      find(Json.obj("active" -> true)).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[Role]

    // gather all the JsObjects in a list
    val futureRolesList: Future[List[Role]] = cursor.collect[List]()

    // transform the list into a JsArray
    val futurePersonsJsonArray: Future[JsArray] = futureRolesList.map { roles => Json.arr(roles) }

    // everything's ok! Let's reply with the array
    futurePersonsJsonArray.map {
      roles =>
        Ok(roles(0))
    }
  }

}
