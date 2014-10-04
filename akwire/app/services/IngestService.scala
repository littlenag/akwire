package services

import akka.actor.{ActorSystem, Props, Actor}
import models.core.ObservedMeasurement
import org.slf4j.{Logger, LoggerFactory}
import models.{RawAlert, RawSubmission}
import scaldi.{Injectable, Injector}

class IngestService(implicit inj: Injector) extends Injectable {

  val actorSystem  = inject[ActorSystem]

  lazy val o = actorSystem.actorOf(Props[ObsHandler], name = "obsHandler");

  def init = {

  }

  def process(submission: RawSubmission) = {
    o ! submission
  }

  def process(alert: RawAlert) = {
    o ! alert
  }

}

class ObsHandler extends Actor {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ObsHandler])

  def tag(measurements: List[ObservedMeasurement]) = {
    measurements
  }

  def receive = {
    case r: RawSubmission =>
      logger.info("Raw submission: " + r.measurements)

      val taggedMeasurements = tag(r.measurements)

      // new observations stream
      //  -> tagger
      //  -> alerting engine
      //  -> stream of alerts
      //
      // observations + alerts
      //  \-> database
      //  \-> delivery service

    case r: RawAlert =>
      logger.info("Alert: " + r)

    case x =>
      logger.info("Received: " + x)
  }
}
