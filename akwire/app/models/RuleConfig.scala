package models

import models.core.Observation
import org.bson.types.ObjectId
import services.AlertContext

/**
 *  An individual rule would inherit from this trait and be constructed by
 *  a RuleBuilder.
 *  
 *  Rules are actually factory objects
 */
trait TriggeringRule {
  def inspect(obs:Observation) //: Stream[ObservedMeasurement] => (Stream[AlertMsg], Option[Stream[ResolvingRule]])
  def unload() : Unit
  def ruleConfig : RuleConfig
}

trait ResolvingRule {
  def inspect(obs:Observation) //: Stream[ObservedMeasurement] => Stream[AlertMsg]
  def unload() : Unit
  def ruleConfig : RuleConfig
}

abstract class RuleBuilder(context: AlertContext) {
  def buildRule(config:RuleConfig) : TriggeringRule
}

// Every RuleConfig generates at most ONE AlertingRule
case class RuleConfig(
  // Maybe create a HydratedRule trait so that I need to pass around Rule with HydratedRule?
  teamId: ObjectId,
  id: ObjectId,
  name: String,

  builder : Class[RuleBuilder],              // class object representing a builder of rules
  params  : Map[String, String],             // kv-pairs that the rule uses to store params

  // FIXME find a better way to keep track of state, e.g. testing vs active
  active: Boolean = true,

  impact: Impact.Value = Impact.IL_5
  //urgency: Urgency.Value = Urgency.UL_5

  // meta: Option[JsObject] = None,              // JSON meta object used by the browser
  // sop: Option[String] = None,                 // wiki link? could take context as an argument, more functional?

  /*
  // the list of fields to compare to tell if Incidents are talking about the same thing
  context:List[String] = List("instance", "host", "observer", "key"),          // for multi-stream and non-ihok-contexted rules

  createdOn: Option[DateTime] = Some(new DateTime()),
  createdBy: Option[ObjectId] = None,                        // id of the user
  lastModifiedOn: Option[DateTime] = Some(new DateTime()),
  lastModifiedBy: Option[ObjectId] = None,                   // id of the user
  */
) {

  // TODO this should be specified by the RuleBuilder
  def context = List("instance", "host", "observer", "key")

  //def impact = Impact.SEV_5
  def urgency = Urgency.UL_5

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

object RuleConfig extends RuleJson

trait RuleJson {
  import JsonUtil._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  import EnumUtils._

  implicit val builderClassFormatter = new Format[Class[RuleBuilder]] {
    override def writes(o: Class[RuleBuilder]): JsValue = {
      JsString(o.getCanonicalName)
    }

    override def reads(json: JsValue): JsResult[Class[RuleBuilder]] = {
      json match {
        case jsString : JsString =>
          this.getClass.getClassLoader.loadClass(jsString.value) match {
            case value: Class[_] if value.isAssignableFrom(classOf[Class[RuleBuilder]])=>
              JsSuccess(value.asInstanceOf[Class[RuleBuilder]])
            case _ =>
              JsError(s"Class ${jsString.value} did not implement RuleBuilder trait")
          }
        case _ => JsError("Expected a string")
      }
    }
  }

  implicit val ruleConfigFormat = Json.format[RuleConfig]
}
