package models

import org.joda.time.DateTime
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import play.api.libs.json._

import models.mongoContext._

// The context should understand both provence and about-ness of the data streams
// Context's tell us when what look like multiple streams are really about the same object
// TODO this needs fixing
case class StreamContext(host: Option[String],
                         observer: Option[String],
                         key: Option[String])

case class Incident( id: ObjectId,

                     // State variables
                     active: Boolean,
                     resolved: Boolean,
                     interred: Boolean,

                     // Lifetime
                     firstTrigger: DateTime,
                     lastTrigger: DateTime,
                     count : Int,

                     // Ownership and Context
                     rule: Rule,
                     teamId : ObjectId,
                     @Key("ctx") streamContext: StreamContext,

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
                 ) extends Contextualized

object Incident extends IncidentDAO with IncidentJson

trait Contextualized {
  val streamContext: StreamContext;
}

trait IncidentDAO extends ModelCompanion[Incident, ObjectId] {
  def collection = MongoConnection()("akwire")("incidents")

  val dao = new SalatDAO[Incident, ObjectId](collection) {}

  // Indexes
  val fields = DBObject(
    "active" -> 1,
    "resolved" -> 1,
    "interred" -> 1,
    "rule.id" -> 1,
    "rule.context" -> 1,
    "ctx.host" -> 1,
    "ctx.observer" -> 1,
    "ctx.key" -> 1
  )

  collection.ensureIndex(fields, "primary_idx")

  // Queries
}

object StreamContext extends StreamContextJson

trait StreamContextJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit val contextFormatter = Json.format[StreamContext]
}

trait IncidentJson extends RuleEnumsJson with StreamContextJson {
  import play.api.libs.json.Json

  import JsonUtil._

  implicit val incidentFormatter = Json.format[Incident]
}
