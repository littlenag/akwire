package engines

import engines.InstructionSet.{Invokation, Instruction}
import models._
import org.bson.types.ObjectId
import org.joda.time.{DateTime}
import org.specs2.matcher.{Matchers, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class PolicyVMTest extends Specification with Mockito {

  "PolicyVM" should {

    val EPOCH = new DateTime(0L)

    class TestListener extends Listener {
      type Step = (Instruction, Registers, Registers)

      val completeHistory = collection.mutable.MutableList.empty[Step]

      val invocations = collection.mutable.MutableList.empty[Instruction]

      override def latchStateChange(instruction: Instruction, newState:Registers, oldState:Registers): Unit = {
        println(s"*$instruction")

        completeHistory += Tuple3(instruction, newState, oldState)

        // Keep track of how many notifications we've done
        if (instruction.isInstanceOf[Invokation]) {
          invocations += instruction
        }
      }
    }


    // logic for escalation and notifications on both sides (alert rules and in the notification/escalation policies)
    // SLA owned by alerting
    // Unresolved state after Policy completes, boolean flag, measure of timeliness

    "simple policy" in {
      running(FakeApplication()) {
        val rule = new Rule(ObjectId.get(), "r1", "...", true, Impact.IL_1)

        val incident = new Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule, ObjectId.get(),
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        val simplePolicy =
          """
            | email user(1)
            | wait 2h
            |
          """.stripMargin

        val programTry = PolicyCompiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        program.instructions must not beEmpty

        program.instructions must have size(3)

        val listener = new TestListener

        val clock = mock[Clock]
        clock.now() returns (EPOCH, EPOCH.plusHours(1), EPOCH.plusHours(2), EPOCH.plusHours(3))

        implicit val vm = new VM(listener, clock)

        val proc : Process = program.instance(incident)

        // need a VM object that
        // owns the clock
        // and handles execution of the instructions

        // the process owns its state

        // load the process, run to completion
        while (proc.tick()) {}

        listener.invocations must have size(1)
        listener.completeHistory must have size(3)
      }
    }

    /**
     | [email user(mark@corp.com), call user(bob@corp.com), text user(carl@corp.com), notify user(thedude@corp.com)]
     | wait 1m
     */

    "repeating policy" in {
      running(FakeApplication()) {

        val rule = new Rule(ObjectId.get(), "r1", "...", true, Impact.IL_1)

        val incident = new Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule, ObjectId.get(),
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        val simplePolicy =
          """
            | attempt 2 times
            | email user(1)
            | wait 1h
            |
          """.stripMargin

        val programTry = PolicyCompiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        program.instructions must not beEmpty

        program.instructions must have size(16)

        val listener = new TestListener

        val clock = mock[Clock]
        var i = 0
        clock.now() answers { _ =>
          i += 10
          EPOCH.plusMinutes(i)
        }

        implicit val vm = new VM(listener, clock)

        val proc : Process = program.instance(incident)

        // load the process, run to completion
        var ticks = 0
        while (proc.tick()) {
          ticks += 1
        }

        //ticks must be equalTo(3)
        listener.invocations must have size(2)
        listener.completeHistory must have size(28)
      }
    }

    "filtering policy" in {
      running(FakeApplication()) {

        val rule = new Rule(ObjectId.get(), "r1", "...", true, Impact.IL_1)

        val incident = new Incident(ObjectId.get(), true, false, false,
          new DateTime(), new DateTime(), 1, rule, ObjectId.get(),
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        //  during(2am to 4am) email user(allincidents@corp.com, rollup)

        // | attempt 2 times every 1h
        val simplePolicy =
          """
            | if incident.impact == IL-1 then
            |   call user(1)
            | end
            |
          """.stripMargin

        val programTry = PolicyCompiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        program.instructions must not beEmpty

        program.instructions must have size(8)

        val listener = new TestListener

        val clock = mock[Clock]
        var i = 0
        clock.now() answers { _ =>
          i += 10
          EPOCH.plusMinutes(i)
        }

        implicit val vm = new VM(listener, clock)

        val proc : Process = program.instance(incident)

        // load the process, run to completion
        var ticks = 0
        while (proc.tick()) {
          ticks += 1
        }

        //ticks must be equalTo(3)
        listener.invocations must have size(1)
        listener.completeHistory must have size(7)
      }
    }

    "matching policy" in {
      running(FakeApplication()) {

        val rule = new Rule(ObjectId.get(), "r1", "...", true, Impact.IL_1)

        val incident = new Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule, ObjectId.get(),
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        // | attempt 2 times
        val simplePolicy =
          """
            | if incident.impact == IL-1 then
            |   call user(1)
            | else
            |   email user(1)
            | end
            | wait 1h
          """.stripMargin

        val programTry = PolicyCompiler.compile(simplePolicy)

        programTry.isRight must beTrue

        val program:Program = programTry.right.get

        program.instructions must not beEmpty

        /**
        program.instructions must have size(15)

        val listener = new TestListener

        val clock = mock[Clock]
        var i = 0
        clock.now() answers { _ =>
          i += 10
          EPOCH.plusMinutes(i)
        }

        implicit val vm = new VM(listener, clock)

        val proc : Process = program.instance(incident)

        // load the process, run to completion
        var ticks = 0
        while (proc.tick()) {
          ticks += 1
        }

        //ticks must be equalTo(3)
        listener.invokations must have size(2)
        listener.latched must have size(26)
        */
      }
    }
  }
}