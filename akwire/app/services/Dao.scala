package services

import javax.inject.Named

import com.mongodb.casbah.MongoConnection
import org.bson.types.ObjectId
import org.slf4j.{LoggerFactory, Logger}
import scaldi.{Injectable, Injector}
import models.{Agent, AgentId}
import play.api.libs.json.{DefaultReads, DefaultWrites, Json}

import scala.concurrent.duration._

import play.api.Play.current

import scala.concurrent.{Await, ExecutionContext}
import ExecutionContext.Implicits.global

class Dao(implicit inj: Injector) extends Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Dao])

  /** The agents collection */
//  private def agents = ReactiveMongoPlugin.db.collection[JSONCollection]("agents")

  import com.novus.salat.dao.SalatDAO

  object AgentsDAO extends SalatDAO[Agent, ObjectId](MongoConnection()("akwire")("agents"))


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
    return AgentsDAO.findOne("agentId" -> agentId.value)
  }
}
