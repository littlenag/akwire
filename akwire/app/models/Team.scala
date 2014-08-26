package models

import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

import models.mongoContext._

// impact is something that the alert knows about itself
object Impact extends Enumeration {
  // CLEAR and INFO are not allowed to have an Urgency attached to them
//  val CLEAR = Value("CLEAR")   // Everything is OK and if anything was wrong in the past its now fixed. Will resolve active situations when received.
//  val INFO = Value("INFO")     // Purely informational in nature, may or may not indicate that anything has gone wrong.

  val SEV5 = Value("SEV5")     // Something small went wrong, and the entity will continue operating.
  val SEV4 = Value("SEV4")     // Something larger went wrong, and the entity will continue operating.
  val SEV3 = Value("SEV3")     // Something went wrong, and the entity may or may not continue.
  val SEV2 = Value("SEV2")     // Something went wrong, and the entity cannot continue.
  val SEV1 = Value("SEV1")     // Something went wrong, and the entity cannot continue.
}

// urgency is something that humans have to know about
object Urgency extends Enumeration {
  val NONE = Value("NONE")
  val LOW = Value("LOW")
  val MEDIUM = Value("MEDIUM")
  val HIGH = Value("HIGH")
  val IMMEDIATE = Value("IMMEDIATE")
}

case class Rule( id: Option[ObjectId],
                 name: String,
                 sop: String,                 // wiki link? could take context as an argument, more functional?
                 impact: Impact.Value,
                 urgency: Urgency.Value,

                 context:String,          // for multi-stream and non-nhok-contexted rules

                 expr : JsObject,         // clojure that's been JSON-encoded

                 createdOn: Option[DateTime],
                 createdBy: Option[User],
                 lastModifiedOn: Option[DateTime],
                 lastModifiedBy: Option[User],

                 //@Ignore team: Option[Team] = None
                 active: Boolean
               ) {

  // in ITIL Priority is a function of both impact and urgency
  // maybe we want to have a priority matrix for each team?
  // then they could select things by priority

  // will want to have a testing life-cycle

  // streams have both provenance and context context
  // provenance tells you what and where the data came from
  // context defines what the data is about
  // rules need to be aware of a stream's context to bucket correctly

  //def makeAlertingRule(): String
  //abstract def handleAlertTrigger(events: List[EventBean]) : List[TriggerAlert]

  // Rules may need to know how to unload themselves
  def destroy = {}
}

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

object Rule extends RuleJson

trait RuleEnumsJson {
  import JsonUtil._
  implicit val impactReader : Reads[Impact.Value] = JsPath.read[Impact.Value]
  implicit val impactWriter : Writes[Impact.Value] = JsPath.write[Impact.Value]
  implicit val urgencyReader : Reads[Urgency.Value] = JsPath.read[Urgency.Value]
  implicit val urgencyWriter : Writes[Urgency.Value] = JsPath.write[Urgency.Value]
}

trait RuleJson extends RuleEnumsJson {
  import JsonUtil._
  implicit val ruleFormatter = Json.format[Rule]
}

trait TeamJson extends RuleJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit val teamFormatter = Json.format[Team]
}
