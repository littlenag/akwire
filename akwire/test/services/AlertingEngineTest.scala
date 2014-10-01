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

      val engine = new AlertingEngine()(DynamicModule { dm =>
        dm.binding to persistence
        dm.binding to getClass.getClassLoader
      })

      engine.init

      // where is a macro, so it has to be in parens
      // other functions that are not macro's do not need to be in parens

      // team id
      val tid = new ObjectId()

      val rule1 = new Rule(tid, "r1", """(where (and (host "h1") (< value 10)) trigger)""")
      val rule2 = new Rule(tid, "r1", """(where (and (host "h1") (> value 11)) trigger)""")

      //val rule = new Rule("r1", """(where (host "h1") trigger)""")
      //val rule = new Rule("r1", """(where true trigger)""")
      //val rule = new Rule("r1", "prn")
      //val rule = new Rule("r1", "akwire.streams/trigger")
      // where not foo resolve

      val team = new Team(tid, "t1", List(rule1), new DateTime(), true)

      engine.loadAlertingRule(team, rule1)

      // v = f(t), v_now = f(t_now)

      val obs = new ObservedMeasurement("i1", "h1", "t1", "k1", 5)

      obs.host mustEqual "h1"

      engine.inspect(obs)

      //there was one(persistence).persistAlert _

      // next rule

      engine.loadAlertingRule(team, rule2)

      // v = f(t), v_now = f(t_now)

      val obs2 = new ObservedMeasurement("i1", "h1", "t1", "k1", 15)

      engine.inspect(obs2)

      there was two(persistence).persistAlert _
    }

    "test alert persistence" in {
      val persistence = new PersistenceService
      persistence mustNotEqual null
    }
  }
}