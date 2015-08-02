package models

import org.bson.types.ObjectId
import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

import models.mongoContext._

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
      (__ \ "ruleData").read[Map[String,String]] ~
      ((__ \ "active").read[Boolean] orElse Reads.pure(true)) ~
      //      (__ \ "meta").readNullable[JsObject] ~
      //      (__ \ "sop").readNullable[String] ~
      ((__ \ "impact").read[Impact.Value] orElse Reads.pure(Impact.IL_5))
    )(Rule.apply _)
}

case class Rule( id: ObjectId,
                 name: String,

                 ruleData : Map[String, String],               // kv-pairs that the rule uses to store params

                 // some way of keeping track of state, like testing a rule, active, deactive
                 active: Boolean = true,

                 //                 meta: Option[JsObject] = None,              // JSON meta object used by the browser
                 //                 sop: Option[String] = None,                 // wiki link? could take context as an argument, more functional?

                 impact: Impact.Value = Impact.IL_5

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
  def urgency = Urgency.UL_5

  // Maybe create a HydratedRule trait so that I need to pass around Rule with HydratedRule?
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

object Rule extends RuleJson
