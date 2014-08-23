package models

import org.joda.time.DateTime

import com.mongodb.casbah.Imports.{ObjectId}

case class Team( id: ObjectId,
                 name: String,
                 //members: Map[ObjectId, User],
                 rules: List[Rule],
                 created: DateTime,
                 active: Boolean)

object Team {
  import play.api.libs.json.Json
  import JsonUtil._
  implicit val teamFormatter = Json.format[Team]
}
