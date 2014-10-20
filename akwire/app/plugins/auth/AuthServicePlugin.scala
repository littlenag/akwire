package plugins.auth

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.Future

//import securesocial.core.providers.Token
//import securesocial.core.{IdentityId, Identity}
import securesocial.core.BasicProfile
import models.User

import play.api.cache._
import play.api.Play.current

import scala.concurrent.duration.Duration

class AuthServicePlugin extends securesocial.core.services.UserService[User] {

  Logger.info(s"Creating AuthServicePlugin")

  def find(providerId : String, userId : String) : Future[Option[BasicProfile]] = {
    Future {
      User.findOneById(new ObjectId(userId)).map(_.profile)
    }
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"Finding identity: ${email} ${providerId}")
    val ident = User.findByEmailAndProvider(email, providerId)

    if (ident.isDefined) {
      Logger.info(s"Found identity for: ${email} ${providerId}")
    }

    ident
  }

  // Receives a SocialUser, and NOT an Akwire User
  def save(profile : securesocial.core.BasicProfile, mode : securesocial.core.services.SaveMode) : scala.concurrent.Future[U] = {
    if (identity.email.isEmpty) {
      throw new Exception("Users MUST have an email!")
    }

    Logger.info(s"Saving new user: ${identity.email.get}")

    // Turn the SocialUser into an Akwire User
    // TODO this seems broken
    val user = new User(ObjectId.get(), identity.email.get, identity.identityId.providerId, identity.fullName, identity.passwordInfo, Nil)
    User.save(user);
    user
  }

  def link(current : U, to : securesocial.core.BasicProfile) : scala.concurrent.Future[U] = {

  }
  def passwordInfoFor(user : U) : scala.concurrent.Future[scala.Option[securesocial.core.PasswordInfo]] = {

  }
  def updatePasswordInfo(user : U, info : securesocial.core.PasswordInfo) : scala.concurrent.Future[scala.Option[securesocial.core.BasicProfile]] = {

  }

  def saveToken(token : securesocial.core.providers.MailToken) : scala.concurrent.Future[securesocial.core.providers.MailToken] = {
    Logger.info(s"Saving token: ${token.uuid}")
    val duration = Duration(token.expirationTime.getMillis - DateTime.now().getMillis, TimeUnit.MILLISECONDS)
    Cache.set(token.uuid, token, duration)
  }
  def findToken(token : scala.Predef.String) : scala.concurrent.Future[scala.Option[securesocial.core.providers.MailToken]] = {
    Logger.info(s"Looking up token: ${uuid}")
    Cache.get(uuid) match {
      case Some(t: Token) => Some(t)
      case _ => None
    }

  }
  def deleteToken(uuid : scala.Predef.String) : scala.concurrent.Future[scala.Option[securesocial.core.providers.MailToken]] = {
    Logger.info(s"Deleteing token: ${uuid}")
    Cache.remove(uuid)

  }

  def deleteExpiredTokens() = {
    // Don't need to do anything in this instance since the Token already
    // has a timeout set when it was saved

  }
}
