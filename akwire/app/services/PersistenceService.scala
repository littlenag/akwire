package services

import akka.actor.Actor
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import models.core.ObservedMeasurement
import models.Incident
import models.alert.{DoTrigger, AlertMsg}
import scaldi.akka.AkkaInjectable
import scaldi.{Injector, Injectable}

import play.api.Logger

class PersistenceService(implicit inj: Injector) extends Injectable {

  private def genIncidentQuery(alert: AlertMsg) : DBObject = {
    val query = MongoDBObject(
      "active" -> true,
      "resolved" -> false,
      "interred" -> false,
      "rule._id" -> alert.rule.id,
      "incident_key" -> alert.contextualizedStream.asDBObject
    )

    return query
  }

  def init = {
    Logger.info("Persistence Services Starting")
  }

  def shutdown = {
    Logger.info("Persistence Services Stopping")
  }

  def persistAlert(alert : DoTrigger) = {
    Logger.info(s"Normal Alert: ${alert}")

    val q = genIncidentQuery(alert)

    Logger.info(s"Looking for incidents matching query: ${q}")

    Incident.findOne(q) match {
      case Some(i) =>
        // TODO Error once we have DoProlong in place
        val next_i = i.increment
        Logger.info(s"Updating incident to: ${next_i}")
        Incident.save(next_i)
        //integrationService.incidentProlong(next_i);
      case None =>
        val i = Incident.fromAlert(alert);
        Logger.info(s"Saving new incident: ${i}")
        Incident.insert(i);
        //integrationService.incidentTriggered(i);
    }
  }

  def persistMeasurement(obs : ObservedMeasurement) = {

  }

}

class PersistenceEngine(implicit inj: Injector) extends Actor with AkkaInjectable {

  val service = inject[PersistenceService]

  def receive = {
    case trigger : DoTrigger => service.persistAlert(trigger)
    case a => Logger.error("Bad message: " + a)
  }
}
