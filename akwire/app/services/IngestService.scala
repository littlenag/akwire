package services

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import models.core.ObservedMeasurement
import models.{RawAlert, RawSubmission}
import play.api.Logger
import scaldi.akka.AkkaInjectable
import scaldi.Injector

class IngestService(implicit inj: Injector) extends AkkaInjectable {

  val actorSystem: ActorSystem = inject[ActorSystem]

  val alerting: AlertingService = inject[AlertingService]

  lazy val obsHandler: ActorRef = actorSystem.actorOf(Props(classOf[ObsHandler], alerting), name = "obsHandler")

  def process(submission: RawSubmission): Unit = {
    obsHandler ! submission
  }

  def process(alert: RawAlert): Unit = {
    obsHandler ! alert
  }
}

class ObsHandler(alerting: AlertingService) extends Actor {

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
