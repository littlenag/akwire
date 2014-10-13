package models

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import securesocial.core._

import models.mongoContext._
import securesocial.core.providers.UsernamePasswordProvider

//
case class TeamRef(id: ObjectId,name: String)

case class User(id: ObjectId,                     // object id, unique for this object for this database
                mail: String,                     // email is also unique, but may be changed (we keep the id around for this reason)
                provider: String,                 // auth provider
                name: String,
                pwdInfo: Option[PasswordInfo],
                profile : BasicProfile,
                memberOfTeams: List[TeamRef]) {   // password hash, id of the hasher algo, and the salt used

/*
  override def identityId: IdentityId = IdentityId(mail, provider)
  override def email: Option[String] = Some(mail)
  override def firstName: String = name
  override def lastName: String = ""
  override def fullName: String = name
  override def oAuth1Info: Option[OAuth1Info] = None
  override def oAuth2Info: Option[OAuth2Info] = None
  override def avatarUrl: Option[String] = None
  override def passwordInfo: Option[PasswordInfo] = pwdInfo
  override def authMethod: AuthenticationMethod = AuthenticationMethod.UserPassword
*/
}

object User extends UserDAO with UserJson {
  def AKWIRE_ADMIN_ACCT_NAME = "admin@akwire.com"
  def AKWIRE_ADMIN_PROVIDER = UsernamePasswordProvider.UsernamePassword
}

trait UserDAO extends ModelCompanion[User, ObjectId] {
  def collection = MongoConnection()("akwire")("users")

  val dao = new SalatDAO[User, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(MongoDBObject("email" -> 1), "name", unique = true)

  // Queries
  def findOneByName(name: String): Option[User] = dao.findOne(MongoDBObject("name" -> name))

  def findByEmailAndProvider(email: String, providerId: String): Option[User] = dao.findOne(MongoDBObject("mail" -> email, "provider" -> providerId))
}

trait UserJson {
  import play.api.libs.json.Json
  import JsonUtil._

  implicit val passwordInfoFormat = Json.format[PasswordInfo]

  implicit val teamRef = Json.format[TeamRef]

  // Generates Writes and Reads for Beans thanks to Json Macros
  implicit val userFormat = Json.format[User]
}
