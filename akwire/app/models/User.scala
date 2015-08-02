package models

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import securesocial.core._

import models.mongoContext._
import securesocial.core.providers.UsernamePasswordProvider

case class TeamRef(id: ObjectId,name: String)

case class User(id: ObjectId,                     // object id, unique for this object for this database
                profile : BasicProfile,
                memberOfTeams: List[TeamRef])

object User extends UserDAO with UserJson {
  def AKWIRE_ADMIN_USERNAME = "admin@akwire.com"
  def AKWIRE_ADMIN_PASSWORD = "admin"
  def AKWIRE_ADMIN_PROVIDER = UsernamePasswordProvider.UsernamePassword
}

trait UserDAO extends ModelCompanion[User, ObjectId] {
  def collection = MongoConnection()("akwire")("users")

  val dao = new SalatDAO[User, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(MongoDBObject("email" -> 1), "name", unique = true)

  // Queries
  def findOneByName(name: String): Option[User] = dao.findOne(MongoDBObject("name" -> name))

  def findByEmailAndProvider(email: String, providerId: String): Option[User] = dao.findOne(MongoDBObject("profile.email" -> email, "profile.providerId" -> providerId))

  def findByUserId(id : ObjectId) = dao.findOneById(id)
}

trait UserJson {
  import play.api.libs.json.Json
  import JsonUtil._

  implicit val authMethodWrites = Json.writes[securesocial.core.AuthenticationMethod]
  implicit val oauth1Writes = Json.writes[securesocial.core.OAuth1Info]
  implicit val oauth2Writes = Json.writes[securesocial.core.OAuth2Info]
  implicit val pwWrites = Json.writes[securesocial.core.PasswordInfo]

  implicit val profileWrites = Json.writes[BasicProfile]

  implicit val teamRef = Json.format[TeamRef]

  // Generates Writes and Reads for Beans thanks to Json Macros
  implicit val userWrites = Json.writes[User]
}
