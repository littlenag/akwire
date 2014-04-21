package services

import javax.inject.Named

import org.slf4j.{LoggerFactory, Logger}
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty
import reactivemongo.api.DefaultDB
import play.modules.reactivemongo.json.collection.JSONCollection
import models.{Agent, AgentId}
import play.api.libs.json.{DefaultReads, DefaultWrites, Json}

import scala.concurrent.duration._
import play.modules.reactivemongo.ReactiveMongoPlugin

import play.api.Play.current

import scala.concurrent.{Await, ExecutionContext}
import ExecutionContext.Implicits.global

@Named
class Dao extends DefaultWrites with DefaultReads {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Dao])

  // Handles all the database access for other services
  @Autowired
  @BeanProperty
  var db : DefaultDB = null;

  /** The agents collection */
  private def agents = ReactiveMongoPlugin.db.collection[JSONCollection]("agents")

  // users
  // roles
  // agents
  //  - instances of collectors
  // streams

  // collectors

  // create
  // read
  // update
  // delete

//  def agents: JSONCollection = db.collection[JSONCollection]("agents")

  def findAgent(agentId: AgentId) : Option[Agent] = {
    return Await.result(agents.find(Json.obj("agentId" -> agentId.value)).one[Agent], 10 seconds);
  }
}
