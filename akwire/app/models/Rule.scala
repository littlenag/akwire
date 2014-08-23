package models

import org.joda.time.DateTime
import play.api.libs.json._

import com.mongodb.casbah.Imports.{ObjectId}

object Impact extends Enumeration {
  val CLEAR = Value("CLEAR")   // Everything is OK and if anything was wrong in the past its now fixed. Will resolve active situations when received.
  val INFO = Value("INFO")     // Purely informational in nature, may or may not indicate that anything has gone wrong.
  val SEV5 = Value("SEV5")     // Something small went wrong, and the entity will continue operating.
  val SEV4 = Value("SEV4")     // Something larger went wrong, and the entity will continue operating.
  val SEV3 = Value("SEV3")     // Something went wrong, and the entity may or may not continue.
  val SEV2 = Value("SEV2")     // Something went wrong, and the entity cannot continue.
  val SEV1 = Value("SEV1")     // Something went wrong, and the entity cannot continue.
}

object Urgency extends Enumeration {
  val NONE = Value("NONE")
  val LOW = Value("LOW")
  val MEDIUM = Value("MEDIUM")
  val HIGH = Value("HIGH")
  val IMMEDIATE = Value("IMMEDIATE")
}

case class Rule( id: ObjectId,
                 name: String,
                 sop: String,                 // wiki link? could take context as an argument, more functional?
                 impact: Impact.Value,
                 urgency: Urgency.Value,
                 //priority: Priority,        // in ITIL Priority is a function of both impact and urgency

                 //context:RuleContext        // for multi-stream and non-nhok contexted rules

                 rule : JsObject,

                 createdOn: Option[DateTime],
                 createdBy: Option[User],
                 lastModifiedOn: Option[DateTime],
                 lastModifiedBy: Option[User],

                 active: Boolean) {           // will want a testing life-cycle

  // streams have both provenance and context context
  // provenance tells you what and where the data came from
  // context defines what the data is about
  // rules need to be aware of a stream's context to bucket correctly

  //def makeAlertingRule(): String
  //abstract def handleAlertTrigger(events: List[EventBean]) : List[TriggerAlert]
}

object Rule {
  import play.api.libs.json.Json
  import JsonUtil._

  implicit val impactReader : Reads[Impact.Value] = JsPath.read[Impact.Value]
  implicit val impactWriter : Writes[Impact.Value] = JsPath.write[Impact.Value]
  implicit val urgencyReader : Reads[Urgency.Value] = JsPath.read[Urgency.Value]
  implicit val urgencyWriter : Writes[Urgency.Value] = JsPath.write[Urgency.Value]

  implicit val ruleFormatter = Json.format[Rule]
}
