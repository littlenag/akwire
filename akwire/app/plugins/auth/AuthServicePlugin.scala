package plugins.auth

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.joda.time.DateTime
//import org.slf4j.{Logger, LoggerFactory}
import play.api.Logger
import play.api.Application
import securesocial.core.providers.Token
import securesocial.core.{IdentityId, Identity}
import models.User

import play.api.cache._
import play.api.Play.current

import scala.concurrent.duration.Duration

class AuthServicePlugin(application: Application) extends securesocial.core.UserServicePlugin(application) {

  //private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthServicePlugin])

  //logger.info(s"Creating AuthServicePlugin")

  Logger.info(s"Creating AuthServicePlugin")


  // For Akwire the userid is the email address
  def find(id: IdentityId): Option[Identity] = {
    findByEmailAndProvider(id.userId, id.providerId)
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = {
    Logger.info(s"Finding identity: ${email} ${providerId}")
    val ident = User.findByEmailAndProvider(email, providerId)

    if (ident.isDefined) {
      Logger.info(s"Found identity for: ${email} ${providerId}")
    }

    ident
  }

  // Receives a SocialUser, and NOT an Akwire User
  def save(identity: Identity): Identity = {
    if (identity.email.isEmpty) {
      throw new Exception("Users MUST have an email!")
    }

    Logger.info(s"Saving new user: ${identity.email.get}")

    // Turn the SocialUser into an Akwire User
    val user = new User(ObjectId.get(), identity.email.get, identity.identityId.providerId, identity.fullName, identity.passwordInfo)
    User.save(user);
    user
  }

  def save(token: Token): Unit = {
    Logger.info(s"Saving token: ${token.uuid}")
    val duration = Duration(token.expirationTime.getMillis - DateTime.now().getMillis, TimeUnit.MILLISECONDS)
    Cache.set(token.uuid, token, duration)
  }

  def findToken(uuid: String): Option[Token] = {
    Logger.info(s"Looking up token: ${uuid}")
    Cache.get(uuid) match {
      case Some(t: Token) => Some(t)
      case _ => None
    }
  }

  def deleteToken(uuid: String): Unit = {
    Logger.info(s"Deleteing token: ${uuid}")
    Cache.remove(uuid)
  }

  def deleteExpiredTokens(): Unit = {
    // Don't need to do anything in this instance since the Token already
    // has a timeout set when it was saved
  }
}
