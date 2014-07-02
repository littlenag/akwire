package models

import play.api.libs.json._

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import play.api.libs.json.{JsSuccess,JsError}

object JsonUtil {

  implicit object objectIdFormat extends Format[ObjectId] {
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

}
