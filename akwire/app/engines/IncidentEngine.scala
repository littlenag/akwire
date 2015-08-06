package engines

import akka.actor.{Actor, ActorRef}
import com.mongodb.casbah.commons.MongoDBObject
import models.Incident
import models.alert.{AlertMsg, DoTrigger}
import models.core.ObservedMeasurement
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

class IncidentEngine(implicit inj: Injector) extends Actor with AkkaInjectable {

  lazy val notification = inject[ActorRef] ('notificationEngine)

  def receive = {
    case trigger : DoTrigger => persistAlert(trigger)
    case a => Logger.error("Bad message: " + a)
  }

  private def genIncidentQuery(alert: AlertMsg) = {
    MongoDBObject(
      "active" -> true,
      "resolved" -> false,
      "interred" -> false,
      "rule._id" -> alert.rule.id,
      "incident_key" -> alert.contextualizedStream.asDBObject
    )
  }

  def persistAlert(alert : DoTrigger) = {
    Logger.info(s"Normal Alert: $alert")

    val q = genIncidentQuery(alert)

    Logger.info(s"Looking for incidents matching query: $q")

    Incident.findOne(q) match {
      case Some(i) =>
        // TODO Error once we have DoProlong in place
        val next_i = i.increment
        Logger.info(s"Updating incident to: $next_i")
        Incident.save(next_i)
      case None =>
        // We only generate new Incidents here in order to keep the logic synchronized and centralized.
        // Then we can send the
        val incident = Incident.fromAlert(alert)
        notification ! (alert, incident)
        Logger.info(s"Saving new incident: $incident")
        Incident.insert(incident);
    }
  }

  def persistMeasurement(obs : ObservedMeasurement) = {

  }
}
