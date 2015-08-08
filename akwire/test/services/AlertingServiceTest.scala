package services

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestKit, ImplicitSender}
import engines.{IncidentEngine, NotificationEngine}
import models.alert.DoTrigger
import models.core.ObservedMeasurement
import models._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.rules.SimpleThreshold
import scaldi.DynamicModule
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import scaldi.akka.AkkaInjectable

class AlertingServiceTest() extends TestKit(ActorSystem("AlertingSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  "AlertingEngine" must {

    "load a rule" in {

      implicit val module = DynamicModule { implicit dm =>
        dm.bind[ActorSystem] to system

        dm.binding toProvider new IncidentEngine
        dm.binding toProvider new NotificationEngine

        dm.bind[ActorRef] identifiedBy 'incidentEngine to {
          implicit val system = dm.inject[ActorSystem]
          AkkaInjectable.injectActorRef [IncidentEngine]
        }

        dm.bind[ActorRef] identifiedBy 'notificationEngine to {
          implicit val system = dm.inject[ActorSystem]
          AkkaInjectable.injectActorRef [NotificationEngine]
        }

      }

      val engine = new AlertingService()

      engine.init

      running(FakeApplication()) {
        val t1 = Team("t1")
        val s = StreamExpr("i".r, "h".r, "o".r, "k".r)
        val r1 = RuleConfig(OwningEntityRef(t1.id, Scope.TEAM), ObjectId.get(), "test1", SimpleThreshold.builderClass, Map("threshold" -> "2", "op" -> "gt"), s)

        engine.loadAlertingRule(r1)

        val obs = ObservedMeasurement("i", "h", "o", "k", 5)

        engine.inspect(obs)
        //expectMsgType[DoTrigger]
      }
    }
  }
}