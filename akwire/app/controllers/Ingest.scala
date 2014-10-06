package controllers

import models.core.ObservedMeasurement
import org.joda.time.DateTime
import play.api.Configuration
import play.api.mvc._

import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injector, Injectable}
import services.IngestService
import models.{RawAlert, RawSubmission}

class Ingest(implicit inj: Injector) extends Controller with Injectable {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Ingest])

  logger.info("Controller has started")

  var ingestService  = inject[IngestService]

  var configuration = inject[Configuration]

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val measReader : Reads[ObservedMeasurement] =
    ( ((__ \ "timestamp").read[DateTime] orElse Reads.pure(DateTime.now())) ~
      ((__ \ "instance").read[String] orElse Reads.pure(configuration.getString("akwire.instance").get)) ~
      (__ \ "host").read[String] ~
      (__ \ "observer").read[String] ~
      (__ \ "key").read[String] ~
      (__ \ "value").read[Double]
      )(ObservedMeasurement.apply _)

  implicit val submissionReads = Json.reads[RawSubmission]

  // try to accept data from:
  //   collectd (write http plugin)
  //   statsd
  //   diamond
  //   akwire
  //   zabbix agent push
  //   datadog (dd-agent)

  def submitObservations = Action(parse.json) {
    request =>
      request.body.asOpt[RawSubmission] match {
        case Some(submission) =>
          ingestService.process(submission)
          Ok("Received: " + submission)
        case None =>
          Ok("Invalid Observations Document")
      }
  }

  def submitAlert = Action(parse.json) {
    request =>
      request.body.validate[RawAlert].fold(
        valid = alert => {
          ingestService.process(alert)
          Ok("Received: " + alert)
        },
        invalid = (e => BadRequest(e.toString))
      )
  }
}
