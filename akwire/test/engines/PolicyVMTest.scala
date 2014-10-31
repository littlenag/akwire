package engines

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
          | email user(mark@.com)
          |
        """.stripMargin

      PolicyVM.eval()

    }

  }

}