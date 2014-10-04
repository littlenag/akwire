package models

import models.core.ObservedMeasurement

case class RawSubmission(measurements: List[ObservedMeasurement])

object RawSubmission {
  import play.api.libs.json.Json

  implicit val om = ObservedMeasurement.omFormat

  implicit val submitFormat = Json.format[RawSubmission]
}
