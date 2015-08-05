package models

import models.mongoContext._

import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.transformers.CustomTransformer
import models.core.Observation
import org.bson.types.ObjectId
import services.AlertContext

import scala.util.Try
import scala.util.matching.Regex

/**
 *  An individual rule would inherit from this trait and be constructed by
 *  a RuleBuilder.
 *  
 *  Rules are actually factory objects
 */
sealed trait RuleLike {
  def inspect(obs:Observation) //: Stream[ObservedMeasurement] => (Stream[AlertMsg], Option[Stream[ResolvingRule]])
  def unload() : Unit
  def ruleConfig : RuleConfig
}

trait TriggeringRule extends RuleLike
trait ResolvingRule extends RuleLike

abstract class RuleBuilder(context: AlertContext) {
  def buildRule(config:RuleConfig) : TriggeringRule
}

case class StreamExpr(instance:Regex, host:Regex, observer:Regex, key:Regex) {

  //def compileToFlow

  def matches(obs:Observation) : Boolean = {
    instance.findFirstIn(obs.instance).isDefined &&
    host.findFirstIn(obs.host).isDefined &&
    observer.findFirstIn(obs.observer).isDefined &&
    key.findFirstIn(obs.key).isDefined
  }
}

object StreamExpr {
  private val ANY = ".*".r
  val All = new StreamExpr(ANY, ANY, ANY, ANY) {
    def matches = true
  }
}

case class RuleBuilderClass(className:String) {
  assert(Class.forName(className).isInstanceOf[Class[RuleBuilder]])

  val klass = Class.forName(className).asInstanceOf[Class[RuleBuilder]]

  def instantiates(builder:RuleBuilder) : Boolean = {
    // If the two class objects are the same then this klass would instantiate
    // a builder of the same type.
    builder.getClass == klass
  }

  def newInstance(context:AlertContext) : RuleBuilder = {
    klass.getConstructor(classOf[AlertContext]).newInstance(context)
  }
}

// Every RuleConfig generates at most ONE AlertingRule
// Assume for now that this RuleConfig is scoped to a Team
case class RuleConfig(
  // Maybe create a HydratedRule trait so that I need to pass around Rule with HydratedRule?
  owner: OwningEntity,
  id: ObjectId,
  name: String,

  builderClass : RuleBuilderClass,           // class object representing a builder of rules, would be a Class but Salat doesn't support that
  params : Map[String, String],              // kv-pairs that the rule uses to store params

  streamExpr: StreamExpr = StreamExpr.All,   // selects the streams to execute against

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

object RuleConfig extends RuleDAO with RuleJson

trait RuleDAO extends ModelCompanion[RuleConfig, ObjectId] {
  def collection = MongoClient()("akwire")("rules")

  val dao = new SalatDAO[RuleConfig, ObjectId](collection) {}
}

trait RuleJson {
  import JsonUtil._
  import play.api.libs.json._

  implicit val builderClassFormatter = new Format[RuleBuilderClass] {
    override def writes(o: RuleBuilderClass): JsValue = JsString(o.className)
    override def reads(json: JsValue) = Try(JsSuccess(RuleBuilderClass(json.as[String]))).getOrElse(JsError("Expected a string"))
  }

  implicit val streamExprFormat = Json.format[StreamExpr]

  implicit val ruleConfigFormat = Json.format[RuleConfig]
}

object RuleBuilderClassTransformer extends CustomTransformer[RuleBuilderClass, String] {
  def deserialize(klass: String) = RuleBuilderClass(klass)
  def serialize(rbc: RuleBuilderClass) = rbc.className
}

object StreamExprTransformer extends CustomTransformer[StreamExpr, DBObject] {
  def deserialize(dbo: DBObject) = {
    val m = new MongoDBObject(dbo)
    StreamExpr(
        m.as[String]("instance").r,
        m.as[String]("host").r,
        m.as[String]("observer").r,
        m.as[String]("key").r
    )
  }
  def serialize(rbc: StreamExpr) = {
    MongoDBObject(
      "instance" -> rbc.instance.regex,
      "host" -> rbc.host.regex,
      "observer" -> rbc.observer.regex,
      "key" -> rbc.key.regex
    )
  }
}
