package controllers

import play.api.mvc._

import javax.annotation.PostConstruct
import org.slf4j.{Logger, LoggerFactory}
import services.IngestService
import models.RawSubmission
import javax.inject.Named
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty

@Named
class Ingest extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Ingest])

  logger.info("Controller has started")

  @Autowired
  @BeanProperty
  var ingestService : IngestService = null;

  @PostConstruct
  def init = {
  }

  // accept data from:
  //   collectd (write http plugin)
  //   statsd
  //   diamond
  //   akwire
  //   zabbix agent push
  //   datadog (dd-agent)

  def submitObservations = Action(parse.json) { request =>
    request.body.asOpt[RawSubmission] match {
      case Some(submission) =>
        ingestService.process(submission)
        Ok("Received: " + submission)
      case None =>
        Ok("Invalid")
    }
  }
}
