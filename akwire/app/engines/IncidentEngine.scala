package engines

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.mongodb.casbah.commons.MongoDBObject
import models.{ProcessInfo, Incident}
import models.alert.{AlertMsg, DoTrigger}
import models.core.ObservedMeasurement
import modules.Init
import org.bson.types.ObjectId
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

case class ArchiveIncident(id:ObjectId)

class IncidentEngine(implicit inj: Injector) extends Actor with AkkaInjectable {

  lazy val notificationEngine = inject[ActorRef] ('notificationEngine)

  implicit val timeout = Timeout(15 seconds)

  def receive = {
    case Init => Logger.info("Nothing to do")
    case trigger : DoTrigger => persistAlert(trigger)

    case msg : NotificationProcessStarted => Logger.error("This shouldn't be received here: $msg")

    case msg: NotificationProcessCompleted => markNotificationProcessCompleted(msg.pid)

    // Sent by the web controllers
    case archive : ArchiveIncident => archiveIncident(archive.id)

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

  /**
   * FIXME this method can be run async, but does need to return a response since
   * the notification actor shouldn't completely die until it knows this message
   * has been handled. That keeps the Incident and Process tables in sync.
   * @param pid
   */
  def markNotificationProcessCompleted(pid:ObjectId) : Unit = {
    val query = MongoDBObject(
      "active" -> true,
      "resolved" -> false,
      "interred" -> false,
      "notificationProcList.pid" -> pid
    )

    for {
      inc <- Incident.findOne(query)
      pi <- inc.notificationProcesses.get(pid)
    } {
      val newPi = pi.copy(running = false)
      Incident.save(inc.copy(notificationProcList = newPi :: inc.notificationProcList.filterNot(_.pid == pid)))
      Logger.info(s"Notifications completed: $pid")
      return
    }

    // If we reach here, then something bad has happened
    Logger.error(s"Pid not found in Incidents table: $pid")
  }

  def persistAlert(alert : DoTrigger): Unit = {
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

        val startedInfo = Await.result(notificationEngine ? NotifyOwnerReturnPid(alert.rule.owner, incident), Duration.Inf).asInstanceOf[NotificationProcessStarted]

        val incidentWithProc = incident.copy(notificationProcList = List(ProcessInfo(startedInfo.pid, None, running = true)))

        Logger.info(s"Saving new incident: $incidentWithProc")
        Incident.insert(incidentWithProc)
    }
  }

  def persistMeasurement(obs : ObservedMeasurement): Unit = {

  }

  def archiveIncident(id:ObjectId) : Option[Incident]= {
    Logger.info(s"Archiving incident: id=$id")

    Incident.findOneById(id).flatMap { incident =>
      val i = incident.copy(active = false)

      for (procs <- i.notificationProcList) {
        notificationEngine ! HaltNotifications(i.owner, i)
      }

      Incident.save(i)
      Some(i)
    }
  }
}
