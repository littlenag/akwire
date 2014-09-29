package services

import models.core.{ObservedMeasurement}
import models.{Rule, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable._
import scaldi.DynamicModule

class AlertingEngineTest extends Specification with Mockito {

  "AlertingEngine" should {

    "load a rule" in {

      val persistence = mock[PersistenceService]

      val engine = new AlertingEngine()(DynamicModule(
        _.binding to persistence
      ))

      engine.init

      // where is a macro, so it has to be in parens
      // other functions that are not macro's do not need to be in parens

      // team id
      val tid = new ObjectId()

      val rule = new Rule(tid, "r1", """(where (and (host "h1") (> value 2)) trigger)""")
      //val rule = new Rule("r1", """(where (host "h1") trigger)""")
      //val rule = new Rule("r1", """(where true trigger)""")
      //val rule = new Rule("r1", "prn")
      //val rule = new Rule("r1", "akwire.streams/trigger")
      // where not foo resolve

      val team = new Team(tid, "t1", List(rule), new DateTime(), true)

      engine.loadAlertingRule(team, rule)

      // v = f(t), v_now = f(t_now)

      val obs = new ObservedMeasurement("i1", "h1", "t1", "k1", 5)

      obs.host mustEqual "h1"

      engine.inspect(obs)

      there was one(persistence).persistAlert _

      rule mustNotEqual null
    }

    "test alert persistence" in {
      val persistence = new PersistenceService
      persistence mustNotEqual null
    }
  }
}