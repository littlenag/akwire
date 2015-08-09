package engines

import models.notificationvm.InstructionSet.{DELIVER}

import org.slf4j.LoggerFactory
import org.specs2.mock.Mockito
import org.specs2.mutable._

import play.api.test._

class PolicyVMTest extends WithApplication with SpecificationLike with Mockito {

  import PolicyTestHelpers._

  private final val logger = LoggerFactory.getLogger(classOf[PolicyVMTest])

  "PolicyVM" should {

    "simplest policy" in {
      val (process, executed) = compileAndExec("email me", incident_IL_0)

      process.program.instructions must have size 3
      executed.filter(_.isInstanceOf[DELIVER]) must have size 1
      executed must have size 3
    }

    "text number policy" in {
      val (process, executed) = compileAndExec("text number(2062354380)", incident_IL_0)

      process.program.instructions must have size 3
      executed.filter(_.isInstanceOf[DELIVER]) must have size 1
      executed must have size 3
    }

    "simple policy" in {
      val policy =
        """
          | email user(1)
          | wait 2m
          |
        """.stripMargin

      val (process, executed) = compileAndExec(policy, incident_IL_0)

      process.program.instructions must have size 4
      executed.filter(_.isInstanceOf[DELIVER]) must have size 1
      executed must have size 6
    }

    "repeating policy" in {
      val policy =
        """
          | attempt 2 times
          | email user(1)
          | wait 1m
          |
        """.stripMargin

      val (process, executed) = compileAndExec(policy, incident_IL_0)

      process.program.instructions must have size 17
      executed.filter(_.isInstanceOf[DELIVER]) must have size 2
      executed must have size 31
    }

    "filtering policy" in {
      val policy =
        """
          | if incident.impact == IL_0 then
          |   call user(1)
          | end
          |
        """.stripMargin

      val (process, executed) = compileAndExec(policy, incident_IL_0)

      process.program.instructions must have size 8
      executed.filter(_.isInstanceOf[DELIVER]) must have size 1
      executed must have size 7
    }

    "matching policy" in {
      val policy =
        """
          | if incident.impact == IL_1 then
          |   call user(1)
          | else
          |   email user(1)
          | end
          | wait 1h
        """.stripMargin

      val (process, executed) = compileAndExec(policy, incident_IL_0)

      process.program.instructions must have size 15
      executed.filter(_.isInstanceOf[DELIVER]) must have size 1
      executed must have size 26
    }
  }
}
