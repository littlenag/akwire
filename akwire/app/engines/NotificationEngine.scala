package engines

import akka.actor.Actor
import models.alert.{DoResolve, DoTrigger}
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

class NotificationEngine(implicit inj: Injector) extends Actor with AkkaInjectable {
  import models.Team

  def receive = {
    case trigger : DoTrigger =>
      val t = Team.findOneById(trigger.rule.teamId)

      // want to compile the script against the incident
      // re-writing terms as necessary
      // then the script needs to be executable
      // so i'm basically creating a virtual machine for these actions to run on
      // where each action is effectively atomic

      // encode all this handling an actor that takes care of the runtime
      // and saving changes to the runtime

      // pagerduty treats this as a simple list of steps, where
      // each step may have a delay attached
      // and then the whole script just has a repeat counter

      // need a barrier primitive, where all actions need to complete before moving forward
      //  - to implement the primitive will need to be able to trigger a save of the runtime

      // execute after primitive, so act as a delay

      // probably want a clock that ticks against the runtime, asking it to execute the next action

      // probably want to implement this first in the same spirit as pagerduty, as a simple list of actions

      Logger.debug("Trigger : " + trigger)

    case resolve : DoResolve =>
      Logger.debug("Resolve: " + resolve)

      // cancel the execution of the team's notification scriptx

    case msg =>
      Logger.error("Unable to process: " + msg)
  }
}


