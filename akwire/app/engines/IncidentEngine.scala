package engines

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.mongodb.casbah.commons.MongoDBObject
import models.{ProcessInfo, Incident}
import models.alert.{AlertMsg, DoTrigger}
import models.core.ObservedMeasurement
import org.bson.types.ObjectId
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

class IncidentEngine(implicit inj: Injector) extends Actor with AkkaInjectable {

  lazy val notificationEngine = inject[ActorRef] ('notificationEngine)

  implicit val timeout = Timeout(5 seconds)

  def receive = {
    case trigger : DoTrigger => persistAlert(trigger)

    case NotificationProcessCompleted(pid) => markNotificationProcessCompleted(pid)

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

  def markNotificationProcessCompleted(pid:ObjectId) : Unit = {
    val query = MongoDBObject(
      "active" -> true,
      "resolved" -> false,
      "interred" -> false,
      "notificationProcesses.pid" -> pid
    )

    for (inc <- Incident.findOne(query);
         pi : ProcessInfo <- inc.notificationProcesses.get(pid.toString)) {
      val newPi : ProcessInfo = pi.copy(running = false)
      Incident.save(inc.copy(notificationProcesses = inc.notificationProcesses + (pid.toString -> newPi)))
      Logger.info(s"Notifications completed: $pid")
      return
    }

    // If we reach here, then something bad has happened
    Logger.error(s"Pid not found in Incidents table: $pid")
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

        val incident = Incident.fromAlert(alert)

        val startedInfo = Await.result(notificationEngine ? NotifyOwner(alert.rule.owner, incident), Duration.Inf).asInstanceOf[NotificationProcessStarted]

        val incidentWithProc = incident.copy(notificationProcesses = Map(startedInfo.pid.toString -> ProcessInfo(startedInfo.pid, None, running = true)))

        Logger.info(s"Saving new incident: $incidentWithProc")
        Incident.insert(incidentWithProc)
    }
  }

  def persistMeasurement(obs : ObservedMeasurement) = {

  }
}