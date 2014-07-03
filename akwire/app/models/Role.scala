package models

import org.joda.time.DateTime

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._


case class Role( id: ObjectId,
                 name: String,
                 //rules: Map[ObjectId, Rule],
                 created: DateTime,
                 active: Boolean)

object Role {
  import play.api.libs.json.Json
  import JsonUtil._
  implicit val roleFormatter = Json.format[Role]
}
