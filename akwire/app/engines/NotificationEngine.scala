package engines

import akka.actor.{ActorRef, Actor}
import models.{OwningEntityRef, Incident, Policy}

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

  def receive = {
    case NotifyOwnerReturnPid(owner, incident) =>

      val policy = Policy.findDefaultForOwner(owner).getOrElse(throw new RuntimeException(s"No default policy for owner $owner"))

      val policyActor = injectActorRef[ProcessExecutor]

      val compiledProgram = PolicyCompiler.compile(policy).right.getOrElse(throw new RuntimeException(s"Expected a compiling policy for $owner"))

      val process = compiledProgram.instance(incident)

      // This CANNOT be incidentEngine here because that breaks the ask pattern. Instead we ASSUME that its
      // the incidentEngine that is asking, but returning a message to the logical sender (which is actually
      // proxy for incidentEngine)
      sender() ! NotificationProcessStarted(process.id)

      policyActor ! ExecuteProcess(process)

    case ProcessCompleted(proc) =>
      incidentEngine ! NotificationProcessCompleted(proc.id)

    case HaltNotifications(owner, incident) =>
      Logger.debug("Halting notifications for: " + owner)

      // cancel the execution of the team's notification scripts

    case msg =>
      Logger.error("Unable to process: " + msg)
  }
}


