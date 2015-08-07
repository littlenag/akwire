package models

import org.joda.time.DateTime

import com.mongodb.casbah.Imports._

case class Role( _id: ObjectId,
                 name: String,
                 created: DateTime,
                 active: Boolean)

/**
 * Probably want a rights and roles model
 * Assign rights to a team
 * Within a team assign rights to team members
 *
 * Basic roles to encode:
 *  - Super Admin: create new teams, delete teams, basically anything
 *  - Team Admin: configure rules, add/remove members to their team, setup anything team related
 *  - Team Member: incident workflow
 */

object Role {
  import play.api.libs.json.Json
  import JsonUtil._
  implicit val roleFormatter = Json.format[Role]
}
