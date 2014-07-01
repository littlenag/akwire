package models

import org.joda.time.DateTime

case class RawSubmission(measurements: List[ObservedMeasurement])

object RawSubmission {
  import play.api.libs.json.Json

  implicit val om = ObservedMeasurement.omFormat

  implicit val submitFormat = Json.format[RawSubmission]
}

case class ObservedMeasurement(timestamp: Option[DateTime],
                               host: Option[String],
                               observer : String,
                               key : String,
                               value : Double)

object ObservedMeasurement {
  import play.api.libs.json.Json

  implicit val omFormat = Json.format[ObservedMeasurement]
}
