package engines

import models._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class PolicyVMTest extends Specification {

  "PolicyVM" should {

    // logic for escalation and notifications on both sides (alert rules and in the notification/escalation policies)
    // SLA owned by alerting
    // Unresolved state after Policy completes, boolean flag, measure of timeliness

    "simple policy 1" in {

      /**
       * maybe make the filters more like match/case statements
       *
       */

      running(FakeApplication()) {
        val simplePolicy =
          """
            | email user(mark@corp.com)
            |
          """.stripMargin

        val programTry = Compiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        println(s"Program: $program")

        program.instructions must not beEmpty

        program.instructions must have size(2)
      }
    }

    "simple policy 2" in {

      /**
       * maybe make the filters more like match/case statements
       *
       */

      running(FakeApplication()) {
        val simplePolicy =
          """
            | email user(mark@corp.com)
            | wait 2h
            |
          """.stripMargin

        val programTry = Compiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        println(s"Program: $program")

        program.instructions must not beEmpty

        program.instructions must have size(3)

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

        implicit val vm = new VM(clock)

        val proc : Process = program.instance(incident)

        // need a VM object that
        // owns the clock
        // and handles execution of the instructions

        // the process owns its state

        // load the process, run to completion
        val effects = proc.run()

        //val effects = PolicyVM.run(proc).toList

        println(s"EFFECTS: ${effects}")

        effects.dropWhile(! _.isInstanceOf[Stop])

        effects must have size(3)
      }
    }

    "simple policy 3" in {

      /**
       * maybe make the filters more like match/case statements
       *
       */

      running(FakeApplication()) {
        val simplePolicy =
          """
            | email user(mark@corp.com)
            | wait 2h
            | repeat 1 times
            |
          """.stripMargin

        val programTry = Compiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        println(s"Program: $program")

        program.instructions must not beEmpty

        program.instructions must not have size(0)
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