package models

import reactivemongo.bson.{BSONObjectID, BSONValue}
import org.joda.time.DateTime
import play.api.libs.json._

abstract class Rule( id: Option[BSONObjectID],
                 name: String,
                 pattern: String,
                 expression: String,
                 sop: String,
                 createdOn: DateTime,
                 active: Boolean) {           // will want a testing life-cycle
  // impact  -> severity
  // urgency -> priority
  // business impact

  // rule context

  abstract def makeAlertingRule(): String
  //abstract def handleAlertTrigger(events: List[EventBean]) : List[TriggerAlert]
}

case class BasicThreshold( id: Option[BSONObjectID],
                      name: String,
                      pattern: String,
                      expression: String,
                      sop: String,
                      createdOn: DateTime,
                      active: Boolean) extends Rule(id,name,pattern,expression,sop,createdOn,active) {

  override def makeAlertingRule(): String = {
    return String.format("select * from measurements where (%s) and (%s)", pattern, expression);
  }
}

case class CountingThreshold( id: Option[BSONObjectID],
                         name: String,
                         pattern: String,
                         expression: String,
                         sop: String,
                         createdOn: DateTime,
                         active: Boolean,
                         triggerCount:Int,
                         resolveCount:Int) extends Rule(id,name,pattern,expression,sop,createdOn,active) {

  override def makeAlertingRule(): String = {
    if (triggerCount > 1) {
      return String.format("context measurementsByStream select * from pattern [ " +
        " every ( " +
        "  firstObs = measurements( (%1$s) and (%2$s) ) -> " +
        "   (" +
        "     [%3$s] lastObs = measurements((%2$s)) and not measurements(not (%2$s))" +
        "   )" +
        " )" +
        "]", pattern, expression, triggerCount);
    } else {
      return String.format("select * from measurements where (%s) and (%s)", pattern, expression);
    }
  }
}

object Rule {
  import play.api.libs.json.Json

  import play.modules.reactivemongo.json.BSONFormats._

  implicit object OptionBSONObjectIDFormat extends PartialFormat[Option[BSONObjectID]] {
    def partialReads: PartialFunction[JsValue, JsResult[Option[BSONObjectID]]] = {
      case JsObject(("$oid", JsString(v)) +: Nil) => JsSuccess(Some(BSONObjectID(v)))
    }
    val partialWrites: PartialFunction[Option[BSONValue], JsValue] = {
      case Some(oid:BSONObjectID) => Json.obj("$oid" -> oid.stringify)
      case None => Json.obj("$oid" -> BSONObjectID.generate.stringify)
    }
  }

  implicit val basicFormatter = Json.format[BasicThreshold]
  implicit val countingFormatter = Json.format[CountingThreshold]

  implicit object ruleFormat extends Format[Rule] {
    def writes(ts: Rule) = ts match {
      // this will get an implicit Writes[Dog] since d is a Dog
      case r: BasicThreshold => Json.toJson(r)
      // this will get an implicit Writes[Cat] since c is a Cat
      case r: CountingThreshold => Json.toJson(r)
      case r => throw new RuntimeException(s"Unknown rule $r")
    }
    def reads(json: JsValue) = (json \ "type").validate[String] match {
      case r:JsSuccess[String] =>
        r.get match {
          case "basic-threshold" => basicFormatter.reads(json)
          case "counting-threshold" => basicFormatter.reads(json)
          case _ => throw new RuntimeException(s"Unknown rule $r")
        }
      case r:JsError => throw new RuntimeException(s"Malformed, missing type info $r")
    }
  }

}


