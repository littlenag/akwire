package controllers

import models.User
import securesocial.controllers.BaseLoginPage
import play.api.mvc.{ AnyContent, Action }
import play.api.Logger
import securesocial.core.RuntimeEnvironment

class Auth(implicit override val env: RuntimeEnvironment[User]) extends BaseLoginPage[User] {
  override def login: Action[AnyContent] = {
    Logger.debug("using Auth Controller")
    super.login
  }
}
