package models

import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

import models.mongoContext._

// impact is something that the alert knows about itself
object Impact extends Enumeration {
  type Impact = Value
  // CLEAR and INFO are not allowed to have an Urgency attached to them
//  val CLEAR = Value("CLEAR")   // Everything is OK and if anything was wrong in the past its now fixed. Will resolve active situations when received.
//  val INFO = Value("INFO")     // Purely informational in nature, may or may not indicate that anything has gone wrong.

  val SEV_5 = Value("SEV-5")     // Something small went wrong, and the system will continue operating.
  val SEV_4 = Value("SEV-4")     // Something larger went wrong, and the system will continue operating.
  val SEV_3 = Value("SEV-3")     // Something went wrong, and the system may or may not continue.
  val SEV_2 = Value("SEV-2")     // Something went wrong, and the system cannot continue.
  val SEV_1 = Value("SEV-1")     // Something went wrong, and the system cannot continue.

  // these feel like types/levels for alerts
  // DEBUG
  // INFO
  // WARNING
  // ERROR
  // CRITICAL

  //

  // http://wiki.en.it-processmaps.com/index.php/Checklist_Incident_Priority

}

// urgency is something that humans have to know about
object Urgency extends Enumeration {
  type Urgency = Value
  val NONE = Value("NONE")
  val LOW = Value("LOW")
  val MEDIUM = Value("MEDIUM")
  val HIGH = Value("HIGH")
  val IMMEDIATE = Value("IMMEDIATE")
}

trait RuleJson {
  import JsonUtil._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  import EnumUtils._

  implicit val impactFormat = EnumUtils.enumFormat(Impact)
  implicit val urgencyFormat = EnumUtils.enumFormat(Urgency)

  /*
  implicit val impactReader : Reads[Impact.Value] = JsPath.read[Impact.Value]
//  implicit val impactWriter : Writes[Impact.Value] = JsPath.write[Impact.Value]

//  implicit val impactReads: Reads[Impact.Value] = StringReads.map(new ObjectId(_))

  implicit val impactWrites = new Writes[Impact.Value] {
    override def writes(en: Impact.Value) = Json.toJson(en.toString)
  }

  implicit val urgencyReader : Reads[Urgency.Value] = JsPath.read[Urgency.Value]
  implicit val urgencyWriter : Writes[Urgency.Value] = JsPath.write[Urgency.Value]
  */

  implicit val ruleWriter = Json.writes[Rule]

  // FIXME names should match the regex found in PolicyVM
  implicit val ruleReader : Reads[Rule] = (
      ((__ \ "id").read[ObjectId] orElse Reads.pure(ObjectId.get())) ~
      (__ \ "name").read[String] ~
      (__ \ "text").read[String] ~
      ((__ \ "active").read[Boolean] orElse Reads.pure(true)) ~
//      (__ \ "meta").readNullable[JsObject] ~
//      (__ \ "sop").readNullable[String] ~
      ((__ \ "impact").read[Impact.Value] orElse Reads.pure(Impact.SEV_5))
  )(Rule.apply _)
}

case class Rule( id: ObjectId,
                 name: String,

                 text : String,                              // text of the rule to be compiled and run by clojure

                 active: Boolean = true,

//                 meta: Option[JsObject] = None,              // JSON meta object used by the browser
//                 sop: Option[String] = None,                 // wiki link? could take context as an argument, more functional?

                 impact: Impact.Value = Impact.SEV_5

/*
                 urgency: Urgency.Value = Urgency.NONE,

                 // the list of fields that matter
                 context:List[String] = List("instance", "host", "observer", "key"),          // for multi-stream and non-ihok-contexted rules

                 createdOn: Option[DateTime] = Some(new DateTime()),
                 createdBy: Option[ObjectId] = None,                        // id of the user
                 lastModifiedOn: Option[DateTime] = Some(new DateTime()),
                 lastModifiedBy: Option[ObjectId] = None,                   // id of the user
*/
               ) {

  def context = List("instance", "host", "observer", "key")

  //def impact = Impact.SEV_5
  def urgency = Urgency.NONE

  @Ignore var teamId: ObjectId = null

//  def setTeamId(t:ObjectId) = _team = t
//  def teamId = _team

  // in ITIL Priority is a function of both impact and urgency
  // maybe we want to have a priority matrix for each team?
  // then they could select things by priority

  // will want to have a testing life-cycle for rules

  // streams have both provenance and context context
  // provenance tells you what and where the data came from
  // context defines what the data is about
  // rules need to be aware of a stream's context to bucket correctly

  //def makeAlertingRule(): String
  //abstract def handleAlertTrigger(events: List[EventBean]) : List[TriggerAlert]

  // Rules may need to know how to unload themselves from the alerting engine
  def destroy = {}
}

case class Team( id: ObjectId,
                 name: String,
                 //members: Map[ObjectId, User],
                 rules: List[Rule],
                 created: DateTime,
                 active: Boolean)

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
    dao.findOne(MongoDBObject("name" -> name)).map(hydrate(_))
  }

  def hydrate(team:Team) = {
    team.rules.foreach(_.teamId = team.id)
    team
  }
}

object Rule extends RuleJson

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
