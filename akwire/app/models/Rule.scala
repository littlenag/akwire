package models

import org.joda.time.DateTime
import play.api.libs.json._

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import com.novus.salat.annotations._

@Salat
abstract class Rule( id: ObjectId,
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

  def makeAlertingRule(): String
  //abstract def handleAlertTrigger(events: List[EventBean]) : List[TriggerAlert]
}

case class BasicThreshold( id: ObjectId,
                      name: String,
                      pattern: String,
                      expression: String,
                      sop: String,
                      createdOn: DateTime,
                      active: Boolean) extends Rule(id,name,pattern,expression,sop,createdOn,active) {

  override def makeAlertingRule(): String = {
    return format("select * from measurements where (%s) and (%s)", pattern, expression);
  }
}

case class CountingThreshold( id: ObjectId,
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
      return format("context measurementsByStream select * from pattern [ " +
        " every ( " +
        "  firstObs = measurements( (%1$s) and (%2$s) ) -> " +
        "   (" +
        "     [%3$s] lastObs = measurements((%2$s)) and not measurements(not (%2$s))" +
        "   )" +
        " )" +
        "]", pattern, expression, triggerCount);
    } else {
      return format("select * from measurements where (%s) and (%s)", pattern, expression);
    }
  }
}

object Rule {
  import play.api.libs.json.Json
  import JsonUtil._

  implicit val basicFormatter = Json.format[BasicThreshold]
  implicit val countingFormatter = Json.format[CountingThreshold]

  implicit object ruleFormat extends Format[Rule] {
    def writes(ts: Rule) = ts match {
      // this will get an implicit Writes[Dog] since d is a Dog
      case r: BasicThreshold => basicFormatter.writes(r)
      // this will get an implicit Writes[Cat] since c is a Cat
      case r: CountingThreshold => countingFormatter.writes(r)
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


