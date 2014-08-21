package models

import play.api.libs.json._

import com.mongodb.casbah.Imports._
import play.api.libs.json.{JsSuccess,JsError}
import java.util.UUID
import org.bson.types.ObjectId

object JsonUtil {

  implicit object ObjectIdFormat extends Format[ObjectId] {
    override def writes(oid: ObjectId) = Json.toJson(oid.toString)
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

  implicit def mapReader[T] : Reads[Map[ObjectId, T]] = JsPath.read[Map[String, T]].map(m => m.keys.map{k => (new ObjectId(k), m.get(k))}.toMap)
//  implicit val mapWriter : Writes[Map[ObjectId, T]] = JsPath.write[Impact.Value]


  implicit def objectMapFormat[T]() = new Format[Map[ObjectId, T]] {
    override def writes(m: Map[ObjectId, T]) : JsValue = {
      Json.to
      Json.toJson(id.toString)
    }

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


}
