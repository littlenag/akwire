package models

import models.alert.AlertMsg
import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

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

case class ContextualizedStream(fields : Map[String, String])

trait Contextualized {
  def contextualizedStream: ContextualizedStream;
}

case class Incident( id: ObjectId,

                     // State variables
                     active: Boolean,
                     resolved: Boolean,
                     interred: Boolean,

                     // Lifetime
                     firstSeen: DateTime,
                     lastSeen: DateTime,
                     count : Int,

                     // Ownership and Context
                     rule: Rule,
                     teamId : ObjectId,

                     @Key("incident_key") incidentKey: ContextualizedStream,

                     // User data
                     notes: Option[String],
                     url: Option[String],        // Wiki-formatted

                     // Blob for integration adapters to keep their data in
                     integrationData: Option[JsObject],

                     ackedBy: Option[User],
                     ackedWhen: Option[DateTime],

                     archivedBy: Option[User],
                     archivedWhen: Option[DateTime],

                     impact: Impact.Value,
                     urgency: Urgency.Value

                     // other possible data:
                     // tags
                     // all observations between the incident start and the eventual close
                     // alerting streams their observations (for multi-stream)
                 ) extends Contextualized {

  override def contextualizedStream = incidentKey

  def this(alert:AlertMsg) = {
    this(null, true, false, false, new DateTime(), new DateTime(), 1, alert.rule, null, )
  }

  def increment = {
    this.copy(count = this.count + 1, lastSeen = new DateTime())
  }

}

object Incident extends IncidentDAO with IncidentJson

trait IncidentDAO extends ModelCompanion[Incident, ObjectId] {
  def collection = MongoConnection()("akwire")("incidents")

  val dao = new SalatDAO[Incident, ObjectId](collection) {}

  // Indexes
  val fields = DBObject(
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

  //implicit val contextFormatter = Json.format[ContextualizedStream]

  implicit object ctxStreamFormat extends Format[ContextualizedStream] {

    val reader : Reads[ContextualizedStream] =
      (JsPath.read[Map[String, String]]).map(x => ContextualizedStream(x))

    override def reads(json: JsValue): JsResult[ContextualizedStream] = {
      reader.reads(json)
    }

    override def writes(o: ContextualizedStream): JsValue = {
      var ret = Json.obj()
      o.fields.foreach(p => ret = ret + (p._1,JsString(p._2)))
      ret
    }
  }

}

trait IncidentJson extends RuleEnumsJson with StreamContextJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit val incidentFormatter = Json.format[Incident]
}
