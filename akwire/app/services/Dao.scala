package services

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.slf4j.{LoggerFactory, Logger}
import scaldi.{Injectable, Injector}
import models.{mongoContext, Agent, AgentId}

class Dao(implicit inj: Injector) extends Injectable {

  // FIXME move this to using LazyLogging
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Dao])

  /** The agents collection */

  import com.novus.salat.dao.SalatDAO
  import mongoContext._

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
    return AgentsDAO.findOne(MongoDBObject("agentId" -> agentId.value))
  }
}
