package engines

import engines.InstructionSet.Instruction
import models._
import org.bson.types.ObjectId
import org.joda.time.{Duration, DateTime}

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import scala.util.control.Breaks._

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

        var stream = Stream

        val listener = new Listener {
          // Executed before every instruction
          override def pre(instruction: Instruction): Unit = {

          }

          override def wait_complete(duration: Duration, endTime: DateTime): Unit = {

          }

          // Duration of the timeout
          override def wait_start(duration: Duration, startTime: DateTime): Unit = {}

          // Executed after every instruction
          override def post(instruction: Instruction): Unit = {}

          override def wait_continue(duration: Duration, curTime: DateTime): Unit = {}

          // Target to email
          override def email(target: Target): Unit = {}
        }

        val vm = new VM(listener, clock)

        val proc : Process = program.instance(incident)

        // need a VM object that
        // owns the clock
        // and handles execution of the instructions

        // the process owns its state

        // load the process, run to completion
        while (vm.tick(proc))

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