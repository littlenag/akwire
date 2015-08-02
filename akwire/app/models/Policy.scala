package models

import com.mongodb.casbah.MongoConnection
import com.novus.salat.dao.{ModelCompanion, SalatDAO}
import models.mongoContext._
import org.bson.types.ObjectId

// Single simple notification policy, owned by a user (for now)
case class Policy(id:ObjectId, userId: ObjectId, policySource:String, default:Boolean)

object Policy extends PolicyDAO with PolicyJson

trait PolicyDAO extends ModelCompanion[Policy, ObjectId] {
  def collection = MongoConnection()("akwire")("policies")

  val dao = new SalatDAO[Policy, ObjectId](collection) {}

  // Indexes

  // Queries
  //def findOneByName(name: String): Option[User] = dao.findOne(MongoDBObject("name" -> name))

  //def findByUserId(id : ObjectId) = dao.findOneById(id)
}

trait PolicyJson {
  import play.api.libs.json.Json
  import JsonUtil._

  implicit val policyFormat = Json.format[Policy]
}
