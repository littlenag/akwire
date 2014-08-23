package controllers

import play.api.mvc._

import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injector, Injectable}
import services.IngestService
import models.{RawAlert, RawSubmission}

class Ingest(implicit inj: Injector) extends Controller with Injectable {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Ingest])

  logger.info("Controller has started")

  var ingestService  = inject[IngestService]

  // accept data from:
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
