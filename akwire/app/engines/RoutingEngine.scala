package engines

import akka.actor.Actor
import models.alert.{DoResolve, DoTrigger}
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable
import services.PersistenceEngine

class RoutingEngine(implicit inj: Injector) extends Actor with AkkaInjectable {

  val persistence = injectActorRef[PersistenceEngine]
  val notification = injectActorRef[NotificationEngine]

  def receive = {
    case trigger : DoTrigger =>
      Logger.debug("Trigger : " + trigger)
      persistence ! trigger
      notification ! trigger

    case resolve : DoResolve =>
      Logger.debug("Resolve: " + resolve)
      persistence ! resolve
      notification ! resolve

    case msg =>
      Logger.error("Unable to route: " + msg)
  }
}
