package models

import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

import models.mongoContext._

// teams should have some notion of membership
case class Team( name: String,
                 id: ObjectId = ObjectId.get(),
                 rules: List[RuleConfig] = Nil,                  // FIXME remove this
                 created: DateTime = new DateTime(),
                 active: Boolean = true)

object Team extends TeamDAO with TeamJson {
  def AKWIRE_ADMIN_TEAM_NAME = "Akwire Administrators"
}

trait TeamDAO extends ModelCompanion[Team, ObjectId] {
  def collection = MongoClient()("akwire")("teams")

  val dao = new SalatDAO[Team, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("name" -> 1), "team_name", unique = true)

  // Queries
  def findOneByName(name: String): Option[Team] = {
    dao.findOne(MongoDBObject("name" -> name)).map(hydrate)
  }

  def hydrate(team:Team) = {
    val rulesWithTeamId = team.rules.map(r => r.copy(teamId = team.id))
    team.copy(rules = rulesWithTeamId)
  }
}

trait TeamJson extends RuleJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit val teamReads = Json.reads[Team]

  implicit val teamWrites : Writes[Team] = new Writes[Team] {
    def writes(t: Team): JsValue = {
      Json.obj(
        "id" -> t.id,
        "name" -> t.name,
        "rules" -> t.rules,
        "created" -> t.created,
        "active" -> t.active
      )
    }
  }
}
