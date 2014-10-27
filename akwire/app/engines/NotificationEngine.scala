package engines

import akka.actor.Actor
import models.Team
import models.alert.{DoResolve, DoTrigger}
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

class NotificationEngine(implicit inj: Injector) extends Actor with AkkaInjectable {
  def receive = {
    case trigger : DoTrigger =>
      val team = Team.findOneById(trigger.rule.teamId)

      Logger.debug("Trigger : " + trigger)

    case resolve : DoResolve =>
      Logger.debug("Resolve: " + resolve)

      // cancel the execution of the team's notification scriptx

    case msg =>
      Logger.error("Unable to process: " + msg)
  }
}
