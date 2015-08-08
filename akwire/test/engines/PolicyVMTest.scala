package engines

import java.lang.{Process => _}

import models.notificationvm.InstructionSet.DELIVER
import models._
import models.notificationvm.Program
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import org.specs2.mock.Mockito
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import resources.rules.SimpleThreshold
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.{PasswordInfo, AuthenticationMethod, BasicProfile}
import securesocial.core.providers.UsernamePasswordProvider

class PolicyVMTest extends Specification with Mockito {

  val hasher = new PasswordHasher {
    override val id: String = "test"

    override def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean = {
      BCrypt.checkpw(suppliedPassword, passwordInfo.password)
    }

    override def hash(plainPassword: String): PasswordInfo = {
      PasswordInfo(id, BCrypt.hashpw(plainPassword, BCrypt.gensalt(10)))
    }
  }

  val id = ObjectId.get()

  val profile = new BasicProfile(
    UsernamePasswordProvider.UsernamePassword,
    "test@akwire.com",       // userId <-> email
    Some("test"),            // firstname
    Some("user"),            // lastname
    Some("test user"),       // fullname
    Some("test@akwire.com"), // username
    None,
      AuthenticationMethod.UserPassword,
    None, None,
    Some(hasher.hash("password")))

  val testUser = new User(id, profile, ContactInfo(None), List())

  "PolicyVM" should {

    def minuteStepClock = new Clock {
      val clock = {
        def loop(v: DateTime): Stream[DateTime] = v #:: loop(v.plusMinutes(1))
        loop(new DateTime(0L))
      }.iterator

      def now(): DateTime = clock.next()
    }

    // logic for escalation and notifications on both sides (alert rules and in the notification/escalation policies)
    // SLA owned by alerting
    // Unresolved state after Policy completes, boolean flag, measure of timeliness

    "simplest policy" in {
      running(FakeApplication()) {
        val team = Team.apply("t1")

        val rule = RuleConfig(testUser.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

        val incident = Incident(ObjectId.get(), active = true, resolved = false, interred = false, new DateTime(), new DateTime(), 1, rule,
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        val simplePolicy =
          """
            | email me
            |
          """.stripMargin

        val program = Program(simplePolicy)

        program.instructions must not beEmpty

        program.instructions must have size 3

        val process = program.instance(incident)

        implicit val vm = new VM(clock = minuteStepClock)

        // run to completion
        val executed = process.iterator.toList

        executed.filter(_.isInstanceOf[DELIVER]) must have size 1
        executed must have size 3
      }
    }

    /*
    "text number policy" in {
      running(FakeApplication()) {
        val team = Team.apply("t1")

        val rule = RuleConfig(testUser.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

        val incident = Incident(ObjectId.get(), active = true, resolved = false, interred = false, new DateTime(), new DateTime(), 1, rule,
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        val simplePolicy =
          """
            | text number(2062354380)
            |
          """.stripMargin

        val program = Program(simplePolicy)

        program.instructions must not beEmpty

        program.instructions must have size(3)

        val listener = new TestListener

        val clock = mock[Clock]
        clock.now() returns (EPOCH, EPOCH.plusHours(1), EPOCH.plusHours(2), EPOCH.plusHours(3))

        implicit val vm = new VM(listener, clock)

        val proc : notificationvm.Process = program.instance(incident)

        // need a VM object that
        // owns the clock
        // and handles execution of the instructions

        // the process owns its state

        // load the process, run to completion
        while (proc.tick()) {}

        listener.deliveries must have size(1)
        listener.completeHistory must have size(2)
      }
    }

    "simple policy" in {
      running(FakeApplication()) {
        val team = Team.apply("t1")
        val rule = RuleConfig(team.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

        val incident = Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule,
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

        val program = Program(simplePolicy)

        program.instructions must not beEmpty

        program.instructions must have size(3)

        val listener = new TestListener

        val clock = mock[Clock]
        clock.now() returns (EPOCH, EPOCH.plusHours(1), EPOCH.plusHours(2), EPOCH.plusHours(3))

        implicit val vm = new VM(listener, clock)

        val proc : notificationvm.Process = program.instance(incident)

        // need a VM object that
        // owns the clock
        // and handles execution of the instructions

        // the process owns its state

        // load the process, run to completion
        while (proc.tick()) {}

        listener.deliveries must have size(1)
        listener.completeHistory must have size(3)
      }
    }

    /**
     | [email user(mark@corp.com), call user(bob@corp.com), text user(carl@corp.com), notify user(thedude@corp.com)]
     | wait 1m
     */

    "repeating policy" in {
      running(FakeApplication()) {

        val team = Team.apply("t1")
        val rule = RuleConfig(team.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

        val incident = Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule,
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

        val program = Program(simplePolicy)

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

        val proc : notificationvm.Process = program.instance(incident)

        // load the process, run to completion
        var ticks = 0
        while (proc.tick()) {
          ticks += 1
        }

        //ticks must be equalTo(3)
        listener.deliveries must have size(2)
        listener.completeHistory must have size(28)
      }
    }

    "filtering policy" in {
      running(FakeApplication()) {

        val team = Team.apply("t1")
        val rule = RuleConfig(team.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

        val incident = Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule,
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
            | if incident.impact == IL_1 then
            |   call user(1)
            | end
            |
          """.stripMargin

        val program = Program(simplePolicy)

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

        val proc : notificationvm.Process = program.instance(incident)

        // load the process, run to completion
        var ticks = 0
        while (proc.tick()) {
          ticks += 1
        }

        //ticks must be equalTo(3)
        listener.deliveries must have size(1)
        listener.completeHistory must have size(7)
      }
    }

    "matching policy" in {
      running(FakeApplication()) {

        val team = Team.apply("t1")
        val rule = RuleConfig(team.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

        val incident = Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule,
          ContextualizedStream(List(("host", "h1"))),
          Impact.IL_1,
          Urgency.UL_0,
          None,
          None
        )

        // | attempt 2 times
        val simplePolicy =
          """
            | if incident.impact == IL_1 then
            |   call user(1)
            | else
            |   email user(1)
            | end
            | wait 1h
          """.stripMargin

        val program = Program(simplePolicy)

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
    */
  }
}