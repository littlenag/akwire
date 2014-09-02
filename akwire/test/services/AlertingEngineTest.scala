package services

import models.core.{ObservedMeasurement, Observation}
import models.{Rule, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable._

class AlertingEngineTest extends Specification {

  "AlertingEngine" should {

    "load a rule" in {
      val engine = new AlertingEngine
      engine.init

      val rule = new Rule("r1", "println obs")
      val team = new Team(new ObjectId(), "t1", List(rule), new DateTime(), true)

      engine.loadAlertingRule(team, rule)

      val obs = new ObservedMeasurement("i1", "h1", "t1", "k1", 5)

      engine.inspect(obs)

      rule mustNotEqual null
    }
  }
}