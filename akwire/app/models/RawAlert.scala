package models

import org.joda.time.DateTime
import java.util.UUID

import play.api.libs.json._
import play.api.libs.functional.syntax._

object IncidentAction extends Enumeration {
  val Trigger = Value("trigger")
  val Acknowledge = Value("acknowledge")
  val Resolve = Value("resolve")
}

case class IncidentKey(key : Either[String, Map[String, String]])

case class RawAlert( serviceKey: UUID,
                     action: IncidentAction.Value,
                     description: String,
                     incidentKey: IncidentKey,
                     details : JsObject)

object rawAlert {

  import JsonUtil._

  implicit val readRawAlert: Reads[RawAlert] = (
    (__ \ "service_key").read[UUID] and
    (__ \ "action").read[IncidentAction.Value] and
    (__ \ "description").read[String] and
    (__ \ "incident_key").read[IncidentKey] and
    (__ \ "details").read[JsObject]
    )(RawAlert)
}

