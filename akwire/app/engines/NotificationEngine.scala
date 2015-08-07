package engines

import java.lang.{Process => _}
import models.notificationvm.{Program, Process}

import akka.actor.{ActorRef, Actor}
import models.{OwningEntityRef, Incident, Policy}
import modules.Init
import org.bson.types.ObjectId
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

case class NotifyOwnerReturnPid(owner:OwningEntityRef, incident:Incident)
case class HaltNotifications(owner:OwningEntityRef, incident:Incident)

case class NotificationProcessStarted(pid:ObjectId)
case class NotificationProcessCompleted(pid:ObjectId)

class NotificationEngine(implicit inj: Injector) extends Actor with AkkaInjectable {

  lazy val incidentEngine = inject[ActorRef] ('incidentEngine)

  case class RuntimeInfo(process:Process, owner:OwningEntityRef, actor:ActorRef)

  private var procs : List[RuntimeInfo] = Nil

  def receive = {
    case Init =>

      for (proc <- Process.findAll() if !proc.terminated) {
        val policyActor = injectActorRef[ProcessExecutor]
        policyActor ! ExecuteProcess(proc)
        procs = RuntimeInfo(proc, proc.incident.owner, policyActor) :: procs
      }

    // Sent by the incident engine to have us start a Process
    case NotifyOwnerReturnPid(owner, incident) =>

      val policy = Policy.findDefaultForOwner(owner).getOrElse(throw new RuntimeException(s"No default policy for owner $owner"))

      val policyActor = injectActorRef[ProcessExecutor]

      val compiledProgram = Program(policy.policySource)

      val process = compiledProgram.instance(incident)

      // This CANNOT be incidentEngine here because that breaks the ask pattern. Instead we ASSUME that its
      // the incidentEngine that is asking, but returning a message to the logical sender (which is actually
      // proxy for incidentEngine)
      sender() ! NotificationProcessStarted(process.id)

      policyActor ! ExecuteProcess(process)

      procs = RuntimeInfo(process, owner, policyActor) :: procs

      // Sent back by the process to inform us of its completion
    case ProcessCompleted(proc) =>
      incidentEngine ! NotificationProcessCompleted(proc.id)

      // Sent by the incident service to inform us that we can stop trying to notify anyone
    case HaltNotifications(owner, incident) =>
      Logger.debug("Halting notifications for: " + owner)

      procs.find(i => i.owner == owner && i.process.incident.id == incident.id).foreach { i =>
        // cancel the execution of the notification scripts
        i.actor ! EarlyTermination
      }

      procs = procs.filterNot(i => i.owner == owner && i.process.incident.id == incident.id)

    case msg =>
      Logger.error("Unable to process: " + msg)
  }
}


