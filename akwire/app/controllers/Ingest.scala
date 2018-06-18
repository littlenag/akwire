package controllers

import java.util

import models.core.ObservedMeasurement
import org.joda.time.DateTime
import play.api.Configuration
import play.api.mvc._

import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injector, Injectable}
import models.{RawAlert, RawSubmission}

class Ingest(implicit inj: Injector) extends Controller with Injectable  {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Ingest])

  logger.info("Controller has started")

  val configuration: Configuration = inject[Configuration]

  val submissionBus: util.Queue[RawSubmission] = inject[util.Queue[RawSubmission]]

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val defaultInstance: Reads[String] = Reads.pure(configuration.getString("akwire.instance").get)

  implicit val measurementReader: Reads[ObservedMeasurement] =
    (
      ((__ \ "instance").read[String] orElse defaultInstance) ~
      (__ \ "host").read[String] ~      // should be inferred from the sending host
      (__ \ "observer").read[String] ~
      (__ \ "key").read[String] ~
      (__ \ "value").read[Double] ~
      ((__ \ "timestamp").read[DateTime] orElse Reads.pure(DateTime.now()))
    )(ObservedMeasurement.apply _)

  implicit val submissionReads: Reads[RawSubmission] = Json.reads[RawSubmission]

  // try to accept data from:
  //   collectd (write http plugin)
  //   statsd
  //   diamond
  //   akwire agent
  //   zabbix agent push
  //   datadog (dd-agent)

  def submitObservations: Action[JsValue] = Action(parse.json) { request =>
    request.body.asOpt[RawSubmission] match {
      case Some(submission) =>
        if (submissionBus.offer(submission)) {
          Ok("Received: " + submission)
        } else {
          // Tell the clients to back off
          TooManyRequest
        }
      case None =>
        Ok("Invalid Observations Document")
    }
  }

  def submitAlert: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[RawAlert].fold(
      valid = alert => {
        Ok("Received: " + alert)
      },
      invalid = e => BadRequest(e.toString)
    )
  }
}
