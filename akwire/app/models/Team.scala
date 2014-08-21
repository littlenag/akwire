package models

import org.joda.time.DateTime

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._


case class Team( _id: ObjectId,
                 name: String,
                 //members: Map[ObjectId, User],
                 rules: Map[ObjectId, Rule],
                 created: DateTime,
                 active: Boolean)

object Team {
  import play.api.libs.json.Json
  import JsonUtil._
  implicit val teamFormatter = Json.format[Team]
}
