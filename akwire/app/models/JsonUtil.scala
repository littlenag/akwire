package models

import play.api.libs.json._
import java.util.UUID
import org.bson.types.ObjectId

object JsonUtil extends DefaultReads with DefaultWrites {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val objectIdReads: Reads[ObjectId] = StringReads.map(new ObjectId(_))

  implicit val objectIdWrites = new Writes[ObjectId] {
    override def writes(oid: ObjectId) = Json.toJson(oid.toString)
  }

  /*
  implicit object ObjectIdReads extends Reads[ObjectId] {
    override def reads(json: JsValue) = json.validate[String] match {
      case r:JsSuccess[String] =>
        try {
          val oid = new ObjectId(r.get)
          JsSuccess(oid)
        } catch {
          case ex: Exception => JsError(s"Malformed ObjectId: $r")
        }
      case r:JsError => JsError(s"ObjectId must be a string")
    }
  }
  */

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

//  implicit def mapReader[T] : Reads[Map[ObjectId, T]] = JsPath.read[Map[String, T]].map(m => m.keys.map{k => (new ObjectId(k), m.get(k))}.toMap)
//  implicit val mapWriter : Writes[Map[ObjectId, T]] = JsPath.write[Impact.Value]

}

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
