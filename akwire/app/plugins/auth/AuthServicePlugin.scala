package plugins.auth

import org.bson.types.ObjectId
import play.api.Logger
import securesocial.core.providers.MailToken

import scala.concurrent.Future

import securesocial.core.{PasswordInfo, BasicProfile}
import models.User

import securesocial.core.services.SaveMode

class AuthServicePlugin extends securesocial.core.services.UserService[User] {

  Logger.info(s"Creating AuthServicePlugin")

  def find(providerId : String, userId : String) : Future[Option[BasicProfile]] = {
    Logger.info(s"Finding identity: ${userId} ${providerId}")
    Future.successful(User.findOneById(new ObjectId(userId)).map(_.profile))
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"Finding identity: ${email} ${providerId}")
    Future.successful(User.findByEmailAndProvider(email, providerId).map(_.profile))
  }

  def save(profile : BasicProfile, mode : SaveMode) : scala.concurrent.Future[User] = {
    if (profile.email.isEmpty) {
      throw new Exception("Users MUST have an email!")
    }

    mode match {
      case SaveMode.SignUp =>
        Logger.info(s"Saving new user: ${profile.email.get}")
        val newUser = User(ObjectId.get(), profile, Nil)
        User.save(newUser)
        Future.successful(newUser)
      case SaveMode.LoggedIn =>
        Future.failed(new RuntimeException("Only signups for now"))
    }

    // We could allow profile linking here, but I choose not to. Maybe later.
  }

  def link(current : User, to : securesocial.core.BasicProfile) : Future[User] = {
    Future.failed(new RuntimeException("No."))
  }

  def passwordInfoFor(user : User) : Future[Option[PasswordInfo]] = {
    if (user.profile.email.isEmpty) {
      throw new Exception("Users MUST have an email!")
    }

    Future.successful(User.findByEmailAndProvider(user.profile.email.get, user.profile.providerId).flatMap(_.profile.passwordInfo))
  }

  def updatePasswordInfo(profile : User, info : PasswordInfo) : Future[Option[BasicProfile]] = {
    if (profile.profile.email.isEmpty) {
      throw new Exception("Users MUST have an email!")
    }

    User.findByEmailAndProvider(profile.profile.email.get, profile.profile.providerId) match {
      case Some(user) =>
        val newProfile = user.profile.copy(passwordInfo = Some(info))
        val newUser = user.copy(profile = newProfile)
        User.save(newUser)
        Future.successful(Some(newProfile))
      case None =>
        Future.failed(new RuntimeException("No user listed"))
    }
  }

  def saveToken(token : MailToken) : scala.concurrent.Future[MailToken] = {
    Future.failed(new RuntimeException("Later"))
    //Logger.info(s"Saving token: ${token.uuid}")
    //val duration = Duration(token.expirationTime.getMillis - DateTime.now().getMillis, TimeUnit.MILLISECONDS)
    //Cache.set(token.uuid, token, duration)
  }

  def findToken(token : String) : Future[Option[MailToken]] = {
    Future.failed(new RuntimeException("Later"))

    //Logger.info(s"Looking up token: ${uuid}")
    //Cache.get(uuid) match {
    //  case Some(t: Token) => Some(t)
    //  case _ => None
    //}
  }

  def deleteToken(uuid : String) : Future[Option[MailToken]] = {
    Future.failed(new RuntimeException("Later"))

    //Logger.info(s"Deleteing token: ${uuid}")
    //Cache.remove(uuid)
  }

  def deleteExpiredTokens() = {
    // Don't need to do anything in this instance since the Token already
    // has a timeout set when it was saved
  }
}
