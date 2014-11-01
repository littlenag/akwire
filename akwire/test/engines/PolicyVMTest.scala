package engines

import models._
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.concurrent._
import duration._
import org.specs2.mutable._

import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import java.util.concurrent.TimeUnit

class PolicyVMTest extends Specification {

  "PolicyVM" should {

    "compile a simple policy" in {

      val simplePolicy =
        """
          | email user(mark@corp.com)
          | delay 2h
          | email user(chris@corp.com)
          | delay 1h
          | repeat 3 times
          |
        """.stripMargin

      val rule = new Rule(ObjectId.get(), "r1", "...", true, Impact.SEV_1)

      val incident = new Incident(ObjectId.get(), true, false, false, new DateTime(), new DateTime(), 1, rule, ObjectId.get(),
      ContextualizedStream(List(("host", "h1"))),
      Impact.SEV_1,
      Urgency.HIGH,
      None,
      None
      )

      val results = PolicyVM.eval(simplePolicy, incident)

      results must not beEmpty
    }

  }

}