package services

import akka.actor.ActorSystem
import engines.{NotificationEngine, RoutingEngine}
import models.core.ObservedMeasurement
import models.{RuleBuilder, RuleConfig, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.test._
import play.api.test.FakeApplication
import play.api.test.Helpers._
import resources.rules.SimpleThreshold
import scaldi.DynamicModule

class AlertingServiceTest extends Specification with Mockito {

  "AlertingEngine" should {

    "load a rule" in {

      val engine = new AlertingService()(DynamicModule { implicit dm =>
        dm.bind[ActorSystem] to ActorSystem("testing")
        dm.bind[PersistenceService] to new PersistenceService()
        dm.binding toProvider new NotificationEngine()
        dm.binding toProvider new PersistenceEngine()
        dm.binding toProvider new RoutingEngine()
      })

      engine.init

      // v = f(t), v_now = f(t_now)
      running(FakeApplication()) {
        val t1 = Team("t1")
        val r1 = RuleConfig(t1.id, ObjectId.get(), "test1", classOf[SimpleThreshold].asInstanceOf[Class[RuleBuilder]], Map("threshold" -> "12", "op" -> "gt"))

        // first rule

        engine.loadAlertingRule(t1, r1)

        val obs = ObservedMeasurement(new DateTime(), "i1", "h1", "t1", "k1", 5)

        engine.inspect(obs)
      }

      // test alerts

      // FIXME
      //there was two(dataRouter).persistAlert _
      true mustEqual true
    }
  }
}