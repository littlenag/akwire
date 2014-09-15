package models

import org.bson.types.ObjectId

case class AgentId(value: String)

case class Agent( id : ObjectId,             // object id, unique for this object for this database
                  agentId: String,            // unique to this agent, and how we do actual lookups
                  hostName: String,
                  connected: Boolean,         // currently connected?
                  managed: Boolean)           // expected to send data and respond normally?

object Agent {
  import play.api.libs.json.Json
  import JsonUtil._

  implicit val agentFormat = Json.format[Agent]
}
