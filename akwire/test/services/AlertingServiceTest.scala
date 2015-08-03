package services

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestKit, ImplicitSender}
import engines.{NotificationEngine, RoutingEngine}
import models.alert.DoTrigger
import models.core.ObservedMeasurement
import models.{StreamExpr, RuleBuilder, RuleConfig, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.rules.SimpleThreshold
import scaldi.DynamicModule
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class AlertingServiceTest() extends TestKit(ActorSystem("AlertingSpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  "AlertingEngine" must {

    "load a rule" in {

      implicit val module = DynamicModule { implicit dm =>
        dm.bind[ActorSystem] to system
        dm.binding toProvider new NotificationEngine()

        dm.bind[PersistenceService] to new PersistenceService()
        dm.binding toProvider new PersistenceEngine()

        dm.binding toProvider new RoutingEngine()
      }

      val engine = new AlertingService()

      engine.init

      running(FakeApplication()) {
        val t1 = Team("t1")
        val s = StreamExpr("i".r, "h".r, "o".r, "k".r)
        val r1 = RuleConfig(t1.id, ObjectId.get(), "test1", classOf[SimpleThreshold].asInstanceOf[Class[RuleBuilder]], Map("threshold" -> "2", "op" -> "gt"), s)

        // first rule

        engine.loadAlertingRule(t1, r1)

        val obs = ObservedMeasurement(new DateTime(), "i", "h", "o", "k", 5)

        engine.inspect(obs)
        //expectMsgType[DoTrigger]
      }
    }
  }
}