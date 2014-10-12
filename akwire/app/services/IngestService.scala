package services

import akka.actor.{ActorSystem, Props, Actor}
import models.core.ObservedMeasurement
import models.{RawAlert, RawSubmission}
import play.api.Logger
import scaldi.akka.AkkaInjectable
import scaldi.{Injector}

class IngestService(implicit inj: Injector) extends AkkaInjectable {

  val actorSystem  = inject[ActorSystem]

  val alerting = inject[AlertingService]

  lazy val o = actorSystem.actorOf(Props(classOf[ObsHandler], alerting), name = "obsHandler");

  def init = {

  }

  def process(submission: RawSubmission) = {
    o ! submission
  }

  def process(alert: RawAlert) = {
    o ! alert
  }

}

class ObsHandler(val alerting: AlertingService) extends Actor {

  def tag(measurements: List[ObservedMeasurement]) = {
    measurements
  }

  def receive = {
    case r: RawSubmission =>
      Logger.info("Raw submission: " + r.measurements)

      val taggedMeasurements = tag(r.measurements)

      taggedMeasurements.foreach(alerting.inspect(_))

      // new observations stream
      //  -> tagger
      //  -> alerting engine
      //  -> stream of alerts
      //
      // observations + alerts
      //  \-> database
      //  \-> delivery service

    case r: RawAlert =>
      Logger.info("Alert: " + r)

    case x =>
      Logger.info("Received: " + x)
  }
}
