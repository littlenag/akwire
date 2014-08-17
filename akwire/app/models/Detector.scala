package models

import org.joda.time.DateTime

import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._

/**
 * A detector could be a full-blown monitoring engine, like Nagios or PRTG, or could
 * be a simple as a shell script running on a single system. In any case the job of
 * a detector is to alarm when things go pear-shaped.
 */
case class Detector( _id: ObjectId,
                     name: String,
                     created: DateTime,
                     active: Boolean)

object Detector {
  import play.api.libs.json.Json
  import JsonUtil._
  implicit val detectorFormatter = Json.format[Detector]
}
