package services

import akka.actor.{ActorSystem, Props, Actor, ActorRef}
import org.slf4j.{Logger, LoggerFactory}
import models.{ObservedMeasurement, RawSubmission}
import scala.beans.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import javax.inject.Named
import javax.annotation.PostConstruct

@Named
class IngestService() {

  @Autowired
  @BeanProperty
  var actorSystem : ActorSystem = null;

  var o : ActorRef = null

  @PostConstruct
  def init = {
    o = actorSystem.actorOf(Props[ObsHandler], name = "obsHandler");
  }

  def process(submission: RawSubmission) = {
    o ! submission
  }
}

class ObsHandler() extends Actor {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ObsHandler])

  def tag(measurements: List[ObservedMeasurement]) = {
    measurements
  }

  def receive = {
    case RawSubmission(measurements) =>
      logger.info("Raw submission: " + measurements)

      val taggedMeasurements = tag(measurements)



      // new observations stream
      //  -> tagger
      //  -> alerting engine
      //  -> stream of alerts
      //
      // observations + alerts
      //  \-> database
      //  \-> delivery service

    case x =>
      logger.info("Received: " + x)
  }
}
