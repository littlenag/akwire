package models

import akka.actor.ActorRef
import akka.stream.scaladsl.{Sink, Source, Flow}
import models.alert.{AlertMsg, DoResolve, DoTrigger}
import models.mongoContext._

import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import com.novus.salat.transformers.CustomTransformer
import models.core.{ObservedMeasurement, Observation}
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.{Format, JsObject}

import scala.collection.parallel.immutable
import scala.concurrent.duration.{Duration, FiniteDuration}
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
  def ruleConfig : PersistedRuleConfiguration
}

trait TriggeringRule extends RuleLike {
  var incidentEngine: ActorRef = _

  def setUtil(actor: ActorRef) = if (this.incidentEngine != null) { this.incidentEngine = actor }

  def config: PersistedRuleConfiguration

  def triggerAlert(obs:List[Observation]) = {
    Logger.debug("Triggering alert with observations: " + obs)
    incidentEngine ! DoTrigger(ruleConfig, obs)
  }

  def resolveAlert(obs:List[Observation]) = {
    Logger.debug("Resolving alert with observations: " + obs)
    incidentEngine ! DoResolve(ruleConfig, obs)
  }
}

object TriggeringRule {
  def apply[T](logic: => T) = {

  }
}

trait ResolvingRule extends RuleLike


trait RuleFactory {
  import play.api.Logger
  import play.api.libs.json.Json

  // Gently suggest that users of the interface use a case class to hold their data
  type Params <: Product with Serializable

  def logger = Logger

  private val _formatter : Format[Params] = Json.format[Params]

  def trigger(ruleConfig: PersistedRuleConfiguration, obs:Observation) : AlertMsg = trigger(ruleConfig, List(obs))

  def resolve(ruleConfig: PersistedRuleConfiguration, obs:Observation) : AlertMsg = resolve(ruleConfig, List(obs))

  def trigger(ruleConfig: PersistedRuleConfiguration, obs:List[Observation]) : AlertMsg = {
    Logger.debug("Triggering alert with observations: " + obs)
    DoTrigger(ruleConfig, obs)
  }

  def resolve(ruleConfig: PersistedRuleConfiguration, obs:List[Observation]) : AlertMsg = {
    Logger.debug("Triggering alert with observations: " + obs)
    DoResolve(ruleConfig, obs)
  }

  def builderClass = {
    RuleFactoryClassName(classOf[this.type].getCanonicalName)
  }

  def validateParams(validator: => Params => Boolean) = {

  }
}

trait EsperRuleFactory extends RuleFactory {

  def buildRule(builder : => (PersistedRuleConfiguration, Params) => TriggeringRule)

}

trait AkkaStreamsRuleFactory[T] extends RuleFactory {

  type Params = T

  var f1 : Flow[ObservedMeasurement, AlertMsg, Unit] = _

  var f2 : Sink[ObservedMeasurement, Unit] = _

  var f3 : Source[ObservedMeasurement, Unit] = _

  var fout : Source[AlertMsg, Unit] = _

  val out = f3.map(_.value > 30)

  final def buildRule(builder : => (PersistedRuleConfiguration, Params, Source[ObservedMeasurement, Unit]) => Source[Option[AlertMsg], Unit]) = {
    // do stuff with the builder
  }


  def groupedWithin(n: Int, d: FiniteDuration) = {
    require(n > 0, "n must be greater than 0")
    require(d > Duration.Zero)
    new TimerTransformer[Int, Seq[Int]] {
      schedulePeriodically(GroupedWithinTimerKey, d)
      var buf: Vector[Int] = Vector.empty

      def onNext(in: Int) = {
        buf :+= in
        if (buf.size == n) {
          // start new time window
          schedulePeriodically(GroupedWithinTimerKey, d)
          emitGroup()
        } else Nil
      }
      override def onTermination(e: Option[Throwable]) = if (buf.isEmpty) Nil else List(buf)
      def onTimer(timerKey: Any) = emitGroup()
      private def emitGroup(): Seq[Seq[Int]] =
        if (buf.isEmpty) EmptyImmutableSeq
        else {
          val group = buf
          buf = Vector.empty
          List(group)
        }
    })
  }
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

case class RuleFactoryClassName(className:String) {
  assert(Class.forName(className).isInstanceOf[Class[RuleFactory]])

  val klass = Class.forName(className).asInstanceOf[Class[RuleFactory]]

  def instantiates(ruleFactory:RuleFactory) : Boolean = {
    // If the two class objects are the same then klass would instantiate a factory of the same type.
    ruleFactory.getClass == klass
  }

  def newInstance() : RuleFactory = {
    klass.getConstructor().newInstance()
  }
}

// A given PersistedRuleConfiguration can be tied to at most ONE AlertingRule instance
// Assume for now that this RuleConfig is scoped to a Team
case class PersistedRuleConfiguration(
  // Maybe create a HydratedRule trait so that I need to pass around Rule with HydratedRule?
  owner: OwningEntityRef,
  id: ObjectId,
  name: String,

  factoryName : RuleFactoryClassName,        // canonical class name of the factory which builds these rules, would be a Class but Salat doesn't support that
  params : JsObject,                         // JSON object representing the user-set params the rule requires

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

object PersistedRuleConfiguration extends RuleDAO with RuleJson

trait RuleDAO extends ModelCompanion[PersistedRuleConfiguration, ObjectId] {
  def collection = MongoClient()("akwire")("rules")

  val dao = new SalatDAO[PersistedRuleConfiguration, ObjectId](collection) {}
}

trait RuleJson {
  import JsonUtil._
  import play.api.libs.json._

  implicit val builderClassFormatter = new Format[RuleFactoryClassName] {
    override def writes(o: RuleFactoryClassName): JsValue = JsString(o.className)
    override def reads(json: JsValue) = Try(JsSuccess(RuleFactoryClassName(json.as[String]))).getOrElse(JsError("Expected a string"))
  }

  implicit val streamExprFormat = Json.format[StreamExpr]

  implicit val ruleConfigFormat = Json.format[PersistedRuleConfiguration]
}

object RuleBuilderClassTransformer extends CustomTransformer[RuleFactoryClassName, String] {
  def deserialize(klass: String) = RuleFactoryClassName(klass)
  def serialize(rbc: RuleFactoryClassName) = rbc.className
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
