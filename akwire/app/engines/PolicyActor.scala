package engines

import akka.actor.Actor
import akka.actor.Actor.Receive
import scaldi.Injector
import scaldi.akka.AkkaInjectable

class PolicyActor()(implicit inj: Injector) extends Actor with AkkaInjectable {

  override def receive: Receive = {

  }
}
