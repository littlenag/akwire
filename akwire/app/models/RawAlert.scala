package models

import java.util.UUID

import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.RawAlert.IncidentKeyType

object IncidentAction extends Enumeration {
  val Trigger = Value("trigger")
  val Acknowledge = Value("acknowledge")
  val Resolve = Value("resolve")
}


case class IncidentKey(key : IncidentKeyType)

/**
 * These are going to be sent by externally integrated systems, like Nagios and PRTG.
 */
case class RawAlert( service_key: UUID,
                     action: IncidentAction.Value = IncidentAction.Trigger,
                     description: Option[String] = None,
                     incident_key: IncidentKey,
                     details : JsObject)

object RawAlert {

  type IncidentKeyType = Either[String, Map[String, String]]

  import JsonUtil._

  implicit object ActionReader extends Reads[IncidentAction.Value] {
    override def reads(json: JsValue): JsResult[IncidentAction.Value] = {
      val reader : Reads[IncidentAction.Value] = JsPath.read[String].map(IncidentAction.withName)
      reader.reads(json)
    }
  }

  implicit object keyReader extends Reads[IncidentKey] {
    def one = (contentType:String) => IncidentKey(Left(contentType))
    def two = (keys: Map[String, String]) => IncidentKey(Right(keys))

    override def reads(json: JsValue): JsResult[IncidentKey] = {
      val reader : Reads[IncidentKey] =
        (JsPath.read[String]).map(one) or
        (JsPath.read[Map[String, String]]).map(two)
      reader.reads(json)
    }
  }

  implicit val readRawAlert: Reads[RawAlert] = Json.reads[RawAlert]
}

