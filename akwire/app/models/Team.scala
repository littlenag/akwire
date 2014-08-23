package models

import org.joda.time.DateTime
import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models.mongoContext._


case class Team( id: ObjectId,
                 name: String,
                 //members: Map[ObjectId, User],
                 rules: List[Rule],
                 created: DateTime,
                 active: Boolean)

object Team extends TeamDAO with TeamJson

trait TeamDAO extends ModelCompanion[Team, ObjectId] {
  def collection = MongoConnection()("akwire")("teams")

  val dao = new SalatDAO[Team, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("name" -> 1), "team_name", unique = true)

  // Queries
  def findOneByName(name: String): Option[Team] = dao.findOne(MongoDBObject("name" -> name))
}

trait TeamJson {
  import play.api.libs.json.Json
  import JsonUtil._
  implicit val teamFormatter = Json.format[Team]
}
