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

      val rule = new Rule("r1", """(where (host "h1") trigger)""")
      //val rule = new Rule("r1", """(where true trigger)""")
      //val rule = new Rule("r1", "prn")
      //val rule = new Rule("r1", "akwire.streams/trigger")
      // where not foo resolve
      val team = new Team(new ObjectId(), "t1", List(rule), new DateTime(), true)

      engine.loadAlertingRule(team, rule)

      // v = f(t), v_now = f(t_now)

      val obs :ObservedMeasurement = new ObservedMeasurement("i1", "h1", "t1", "k1", 5)

      obs.host mustEqual "h1"

      engine.inspect(obs)

      rule mustNotEqual null
    }
  }
}