package controllers

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import scaldi.{Injectable, Injector}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import play.api.libs.json._
import models._
import services.{CommandResult, Messaging}
import scala.concurrent.duration._

/**
 * The Agents controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 */
class Agents(implicit inj: Injector) extends Controller with Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Agents])

  val messaging =  inject[Messaging]

  import mongoContext._

  object AgentsDAO extends SalatDAO[Agent, ObjectId](MongoConnection()("akwire")("agents"))

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
    Future {
      AgentsDAO.findOne(MongoDBObject("agentId" -> new ObjectId(agentId))) match {
        case Some(agent: Agent) => Ok(Json.toJson(agent))
        case None => NotFound("No agent with id: " + agentId)
      }
    }
  }

  def findAllAgents = Action.async {
    Future {
      Ok(Json.arr(AgentsDAO.find(MongoDBObject.empty).sort(orderBy = MongoDBObject("created" -> -1)).toList))
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
