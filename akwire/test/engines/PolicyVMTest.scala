package engines

import engines.InstructionSet.Instruction
import models._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.concurrent._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class PolicyVMTest extends Specification {

  "PolicyVM" should {

    // logic for escalation and notifications on both sides (alert rules and in the notification/escalation policies)
    // SLA owned by alerting
    // Unresolved state after Policy completes, boolean flag, measure of timeliness

    "compile a simple policy" in {

      /**
       * maybe make the filters more like match/case statements
       *
       */

      running(FakeApplication()) {
        val simplePolicy =
          """
            | email user(mark@corp.com)
            | wait 2h
            | email user(chris@corp.com)
            | wait 1h
            | repeat 3 times
            |
          """.stripMargin

        val resultsTry = PolicyVM.compile(simplePolicy)

        resultsTry must beASuccessfulTry

        val results:List[Instruction] = resultsTry.get

        println(s"Results: $results")

        results must not beEmpty

        results must have size(6)
      }
    }

    "run policy program" in {
      val rule = new Rule(ObjectId.get(), "r1", "...", true, Impact.SEV_1)

      val incident = new Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule, ObjectId.get(),
        ContextualizedStream(List(("host", "h1"))),
        Impact.SEV_1,
        Urgency.HIGH,
        None,
        None
      )

      val clock = new Clock {
        override def now() = new DateTime(0L)
      }

      clock must not beNull
    }
  }
}