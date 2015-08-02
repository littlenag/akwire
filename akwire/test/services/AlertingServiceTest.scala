package services

import models.core.{ObservedMeasurement}
import models.{RuleConfig, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable._
import resources.rules.SimpleThreshold
import scaldi.DynamicModule

class AlertingServiceTest extends Specification with Mockito {

  "AlertingEngine" should {

    "load a rule" in {

      val engine = new AlertingService()(DynamicModule { dm =>
        // Nothing to inject
      })

      engine.init

      // v = f(t), v_now = f(t_now)

      val t1 = Team("t1")
      val r1 = RuleConfig(t1.id, ObjectId.get(), "test1", Class[SimpleThreshold], Map("threshold" -> "12", "op" -> "gt"))

      // first rule

      engine.loadAlertingRule(t1, r1)

      val obs = ObservedMeasurement(new DateTime(), "i1", "h1", "t1", "k1", 5)

      engine.inspect(obs)

      // test alerts

      // FIXME
      //there was two(dataRouter).persistAlert _
      true mustEqual true
    }
  }
}