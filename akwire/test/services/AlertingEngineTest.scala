package services

import models.{Rule, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable._

class AlertingEngineTest extends Specification {

  "AlertingEngine" should {

    "load a rule" in {
      val engine = new AlertingEngine
      engine.init

      val rule = new Rule("r1", "(+ 1 2)")
      val team = new Team(new ObjectId(), "t1", List(rule), new DateTime(), true)

      engine.loadAlertingRule(team, rule)

      // uuid mustNotEqual null
    }
  }
}