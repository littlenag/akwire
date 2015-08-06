package models

import play.api.libs.json._
import java.util.UUID
import org.bson.types.ObjectId

import scala.util.Try
import scala.util.matching.Regex

object EnumUtils {
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(EnumUtils.enumReads(enum), EnumUtils.enumWrites)
  }
}

object JsonUtil extends DefaultReads with DefaultWrites {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val objectIdReads: Reads[ObjectId] = StringReads.map(new ObjectId(_))
  implicit val objectIdWrites = new Writes[ObjectId] {
    override def writes(oid: ObjectId) = Json.toJson(oid.toString)
  }

  implicit object UUIDFormat extends Format[UUID] {
    override def writes(id: UUID) = Json.toJson(id.toString)
    override def reads(json: JsValue) = json.validate[String] match {
      case r:JsSuccess[String] =>
        try {
          JsSuccess(UUID.fromString(r.get))
        } catch {
          case ex: Exception => JsError(s"Malformed UUID: $r")
        }
      case r:JsError => JsError(s"UUID must be a string")
    }
  }

  implicit object RegexFormat extends Format[Regex] {
    override def writes(regex: Regex) = Json.toJson(regex.regex)
    override def reads(json: JsValue) = Try(JsSuccess(json.as[String].r)).getOrElse(JsError(s"Malformed Regex: $json"))
  }


  implicit val impactFormat = EnumUtils.enumFormat(Impact)
  implicit val urgencyFormat = EnumUtils.enumFormat(Urgency)

  implicit val scopeFormat = EnumUtils.enumFormat(Scope)
  implicit val owningEntityFormat = Json.format[OwningEntityRef]
}

