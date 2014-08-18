package models

import org.joda.time.DateTime

import com.mongodb.casbah.Imports._
import java.util.UUID
import play.api.libs.json.{DefaultReads, DefaultWrites}

/**
 * A detector could be a full-blown monitoring engine, like Nagios or PRTG, or could
 * be a simple as a shell script running on a single system. In any case the job of
 * a detector is to alarm when things go pear-shaped.
 */
case class Detector( _id: ObjectId,
                     name: String,
                     created: DateTime,
                     integrationToken : Option[UUID],
                     active: Boolean)

object Detector {
  import play.api.libs.json._
//  import com.novus.salat._
//  import com.novus.salat.global._

  import models.JsonUtil._
  implicit val detectorFormatter = Json.format[Detector]
  //import play.api.libs.json.DefaultFormat
  //import play.api.libs.json.DefaultReads
  //import play.api.libs.json.DefaultWrites
}
