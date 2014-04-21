package models

case class User( age: Int,
                 firstName: String,
                 lastName: String,
                 active: Boolean)

case class AgentId(value: String)

case class Agent( agentId: String,
                  hostName: String,
                  connected: Boolean,         // currently connected?
                  managed: Boolean)           // expected to send data and respond normally?

object User {
  import play.api.libs.json.Json

  // Generates Writes and Reads for Beans thanks to Json Macros
  implicit val userFormat = Json.format[User]
}

object Agent {
  import play.api.libs.json.Json

  implicit val agentFormat = Json.format[Agent]
}