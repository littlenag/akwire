package controllers

import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import javax.inject.{Inject, Named}
import play.api.mvc._
import play.api.libs.json._
import models._
import services.{CommandResult, Messaging}
import scala.concurrent.duration._
import play.api.http.DefaultWriteables
import scala.util.{Success, Failure}

/**
 * The Agents controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 */
@Named
class Agents @Inject() (messaging: Messaging) extends Controller with MongoController with DefaultWriteables {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Agents])

  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("agents")

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  def updateAgent = Action.async(parse.json) {
    request =>
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
      request.body.validate[Agent].map {
        agent => Future.successful(Ok("parsed: " + agent))
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findOneAgent(agentId:String) = Action.async {
    val cursor: Future[Option[Agent]] = collection.
      // find all
      find(Json.obj("agentId" -> agentId)).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      one[Agent]

    cursor.map {
      case Some(agent) => Ok(Json.toJson(agent))
      case None => NotFound("No agent with id: " + agentId)
    }
  }

  def findAllAgents = Action.async {
    // let's do our query
    val cursor: Cursor[Agent] = collection.
      // find all
      find(Json.obj()).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[Agent]

    // gather all the JsObjects in a list
    val futureAgentsList: Future[List[Agent]] = cursor.collect[List]()

    // transform the list into a JsArray
    val futurePersonsJsonArray: Future[JsArray] = futureAgentsList.map { agents =>
      Json.arr(agents)
    }
    // everything's ok! Let's reply with the array
    futurePersonsJsonArray.map {
      agents =>
        Ok(agents(0))
    }
  }

  implicit val timeout = akka.util.Timeout(30 seconds)
  implicit val crFormat = play.api.libs.json.Json.format[CommandResult]

  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import scala.concurrent.duration._

  def queryAgentX(agentId:String, command:String) = Action.async {
    val f = messaging.invokeCommand(AgentId(agentId),command)

    val timeoutFuture = play.api.libs.concurrent.Promise.timeout(InternalServerError("timeout"), 1.second)

    Future.firstCompletedOf(Seq(f, timeoutFuture)).map {
      case cr:CommandResult => Ok(Json.toJson(cr))
      case st:Status => st
    }
  }

  def queryAgent(agentId:String, command:String) = Action.async {
    val f = messaging.invokeCommand(AgentId(agentId),command)

    f.transform(c => Ok(Json.toJson(c)), t => t)

//      case Failure(t) => return InternalServerError("")
  }
}
