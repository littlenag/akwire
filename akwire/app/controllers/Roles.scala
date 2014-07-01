package controllers

import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import javax.inject.Named
import play.api.mvc._
import play.api.libs.json._

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
     */
      request.body.validate[Role].map {
        role =>
          collection.insert(role).map {
            lastError =>
              logger.debug(s"Successfully create Role with LastError: $lastError")
              Created(s"Role Created")
          }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findRole = Action.async {
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
