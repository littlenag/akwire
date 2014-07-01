package models

import reactivemongo.bson.BSONObjectID
import org.joda.time.DateTime

case class Role( id: Option[BSONObjectID],
                 name: String,
                 rules: Map[BSONObjectID, Rule],
                 createdOn: DateTime,
                 active: Boolean)

object Role {
  import play.api.libs.json.Json



  implicit val roleFormatter = Json.format[Role]
}
