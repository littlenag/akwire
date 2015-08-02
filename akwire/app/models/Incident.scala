package models

import com.novus.salat.transformers.CustomTransformer
import models.alert.AlertMsg
import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject

import models.mongoContext._

/*
 * The context tells you which fields to compare to distinguish two streams from each other.
 * This is important because two streams may have differing fields but still be about the same object
 * from the perspective of the user.
 *
 * The context should understand both provence and about-ness of the data streams
 *
 * TODO this needs fixing
 *
 * only host, instance, and cluster should be privileged fields
 *
 * stream name + rule context -> contextualized stream
 *
 * incident key is actually a contextualized stream
 */

// Pairs of field name and its content, order matters to mongo so its preserved
// as part of the ordering inherent in a List
case class ContextualizedStream(fields : List[(String, String)]) {

  def asDBObject : DBObject = {
    val o = MongoDBObject().empty

    fields.foreach { p =>
      o.put(p._1, p._2)
    }

    return o
  }
}

object ContextualizedStreamTransformer extends CustomTransformer[ContextualizedStream, DBObject] {
  def deserialize(b: DBObject) = {
    val l = for (k <- b.keys) yield (k, b.get(k).asInstanceOf[String])
    new ContextualizedStream(l.toList)
  }
  def serialize(a: ContextualizedStream) = a.asDBObject
}

trait Contextualized {
  def contextualizedStream: ContextualizedStream;
}

case class Incident( id: ObjectId,

                     // State variables
                     active: Boolean,
                     resolved: Boolean,       // auto-resolve
                     interred: Boolean,       // still relevant?

                     // Lifetime
                     firstSeen: DateTime,
                     lastSeen: DateTime,
                     count : Int,

                     // Ownership and Context
                     rule: RuleConfig,
                     teamId : ObjectId,

                     @Key("incident_key") incidentKey: ContextualizedStream,

                     impact: Impact.Value,              // set by the Rule
                     urgency: Urgency.Value,            // initial value set by the Rule

                     // User data
                     notes: Option[String] = None,
                     url: Option[String] = None,        // Wiki-formatted

                     // Blob for integration adapters to keep their data in
                     integrationData: Option[JsObject] = None,

                     ackedBy: Option[User] = None,
                     ackedWhen: Option[DateTime] = None,

                     archivedBy: Option[User] = None,
                     archivedWhen: Option[DateTime] = None

                     // other possible data:
                     // tags
                     // all observations between the incident start and the eventual close
                     // alerting streams their observations (for multi-stream)
                 ) extends Contextualized {

  override def contextualizedStream = incidentKey

  def increment = {
    this.copy(count = this.count + 1, lastSeen = new DateTime())
  }

}

object Incident extends IncidentDAO with IncidentJson {

  /**
   * Salat doesn't like multiple constructors with arguments
   * @param alert
   * @return
   */
  def fromAlert(alert:AlertMsg) = {
    new Incident(null, true, false, false, new DateTime(), new DateTime(), 1, alert.rule, alert.rule.teamId, alert.contextualizedStream, alert.rule.impact, alert.rule.urgency)
  }
}

trait IncidentDAO extends ModelCompanion[Incident, ObjectId] {
  def collection = MongoConnection()("akwire")("incidents")

  val dao = new SalatDAO[Incident, ObjectId](collection) {}

  // Indexes
  val fields = MongoDBObject(
    "active" -> 1,
    "resolved" -> 1,
    "interred" -> 1,
    "rule.id" -> 1,
//    "rule.context" -> 1,
    "incident_key" -> 1
  )

  collection.ensureIndex(fields, "primary_idx")

  // Queries
}

object ContextualizedStream extends StreamContextJson

trait StreamContextJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit object ctxStreamFormat extends Format[ContextualizedStream] {

    // We want objects who are really just a simple set of key-value pairs:
    // { k1:"v1", k2:"v2",...}
    override def reads(json: JsValue): JsResult[ContextualizedStream] = {
      json.validate[JsObject] match {
        case JsSuccess(ob, path) =>
          ob.fields.find(p => ! p._2.isInstanceOf[JsString]).map { o =>
            return JsError(s"Field ${o._1} must have a string value; value ${o._2} is not a string")
          }
          val l = ob.fields.toList.map(p => (p._1, p._2.as[JsString].value))
          return JsSuccess(ContextualizedStream(l))
        case e : JsError =>
          return e ++ JsError("Expected an object")
      }
    }

    override def writes(o: ContextualizedStream): JsValue = {
      var ret = Json.obj()
      o.fields.foreach(p => ret = ret + (p._1,JsString(p._2)))
      ret

      //o.fields.foldLeft(Json.obj()) { (obj,p) =>
      //  obj + (p._1,JsString(p._2))
      //}
    }
  }

}

trait IncidentJson extends StreamContextJson with UserJson with RuleJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit val incidentWriter = Json.writes[Incident]
}
