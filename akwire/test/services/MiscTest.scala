package services

import org.specs2.mock.Mockito
import org.specs2.mutable._
import securesocial.core.PasswordInfo
import securesocial.core.providers.utils.PasswordHasher

import play.api.test.FakeApplication
import play.api.test.Helpers._

class MiscTest extends Specification with Mockito {
  "MiscTest" should {

    "hash a password" in {

      running(FakeApplication()) {
        val default = new PasswordHasher.Default()

        val pwdInfo1 = default.hash("admin")
        val pwdInfo2 = new PasswordInfo("bcrypt", "$2a$10$/cbhFxAtElCN2VkB.wOIXOA8oHTWlOAkBjtxg7.yXpdlZx8gqfxgG", None)

        default.matches(pwdInfo2, "admin") mustEqual true

        //mustEqual "$2a$10$BhM2DrUWmTLg1TLSboGbe.xRvsDoGGnKb7/XPO.fLrgWFrn.PFssi"
      }
    }
  }
}
