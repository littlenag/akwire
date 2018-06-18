package engines

import java.lang.{Process => _}
import models.notificationvm.Process

import models.notificationvm.InstructionSet.Instruction
import models._
import models.notificationvm.Program
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt

import play.api.test._
import resources.rules.SimpleThreshold
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.{PasswordInfo, AuthenticationMethod, BasicProfile}
import securesocial.core.providers.UsernamePasswordProvider

object PolicyTestHelpers extends WithApplication {

  val hasher = new PasswordHasher {
    override val id: String = "test"

    override def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean = {
      BCrypt.checkpw(suppliedPassword, passwordInfo.password)
    }

    override def hash(plainPassword: String): PasswordInfo = {
      PasswordInfo(id, BCrypt.hashpw(plainPassword, BCrypt.gensalt(10)))
    }
  }

  val testUser = new User(
    ObjectId.get(),
    new BasicProfile(
      UsernamePasswordProvider.UsernamePassword,
      "test@akwire.com", // userId <-> email
      Some("test"), // firstname
      Some("user"), // lastname
      Some("test user"), // fullname
      Some("test@akwire.com"), // username
      None,
      AuthenticationMethod.UserPassword,
      None, None,
      Some(hasher.hash("password"))
    ),
    ContactInfo(None), List()
  )

  val ruleSimpleThreshold = RuleConfig(testUser.asRef, ObjectId.get(), "r1", SimpleThreshold.builderClass, Map.empty[String, String])

  val incident_IL_0 = Incident(
    ObjectId.get(),
    active = true,
    resolved = false,
    interred = false,
    new DateTime(),
    new DateTime(),
    1,
    ruleSimpleThreshold,
    ContextualizedStream(List(("host", "h1"))),
    Impact.IL_0,
    Urgency.UL_0,
    None,
    None
  )


  def minuteStepClock = new Clock {
    val maxRuntime = 120
    val start = new DateTime(0L)
    val clock = {
      def loop(v: DateTime): Stream[DateTime] = {
        if (start.plusMinutes(maxRuntime).isAfter(v))
          v #:: loop(v.plusMinutes(1))
        else
          throw new RuntimeException("Max runtime of clock exceeded, check program")
      }
      loop(start)
    }.iterator

    def now(): DateTime = clock.next()
  }

  // logic for escalation and notifications on both sides (alert rules and in the notification/escalation policies)
  // SLA owned by alerting
  // Unresolved state after Policy completes, boolean flag, measure of timeliness

  def compileAndExec(policy:String, incident: Incident) : (Process,List[Instruction]) = {
    implicit val vm = new VM(clock = minuteStepClock)

    val process = Program(policy).instance(incident)

    (process, process.iterator.toList)
  }
}
