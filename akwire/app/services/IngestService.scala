package services

import akka.actor.ActorSystem
import models.core.ObservedMeasurement
import models.RawSubmission
import scaldi.akka.AkkaInjectable
import scaldi.Injector

class IngestService(implicit inj: Injector) extends AkkaInjectable {

  implicit val actorSystem: ActorSystem = inject[ActorSystem]

  val alerting: AlertingService = inject[AlertingService]

  // new observations stream
  //  -> tagger
  //  -> alerting engine
  //  -> stream of alerts
  //
  // observations + alerts
  //  \-> database
  //  \-> delivery service
  def processObservations(submission: RawSubmission) = {
    submission.measurements
      .map(tag)
      .foreach(alerting.inspect(_))
  }


  def tag(rs:ObservedMeasurement): ObservedMeasurement = rs
}
