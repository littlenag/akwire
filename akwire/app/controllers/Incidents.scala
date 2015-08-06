package controllers

import com.mongodb.casbah.commons.MongoDBObject
import models._
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json
import scaldi.{Injector, Injectable}
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import services.CoreServices

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import Binders._

class Incidents(implicit inj: Injector, override implicit val env: RuntimeEnvironment[User]) extends SecureSocial[User] with Injectable {

  val core = inject[CoreServices]

  def queryIncidents(entity:Option[OwningEntityRef]) = SecuredAction.async { request =>
    Future {
      Logger.info(s"Querying incidents: entity=$entity")

      // TODO check user access to entity (team,user,service)

      val filter = MongoDBObject.empty

      if (entity.isDefined) {
        filter.putAll(MongoDBObject("owner._id" -> entity.get.id, "owner.scope" -> entity.get.scope.toString))
      }

      val sort = MongoDBObject("name" -> 1)
      val list = Incident.find(filter).sort(sort).toList

      Ok(Json.toJson(list))
    }
  }

  def getIncident(id:ObjectId) = SecuredAction.async { request =>
    Future {
      Logger.info(s"Querying incidents: id=$id")

      // TODO check user access to entity (team,user,service)

      val filter = MongoDBObject("_id" -> id)
      val sort = MongoDBObject("name" -> 1)

      val list = Incident.find(filter).sort(sort).toList

      Ok(Json.toJson(list))
    }
  }

  /**
   * For now just return the Incident as is, but implement lifecycle actions eventually.
   * @param id
   * @return
   */
  def updateIncident(id:ObjectId) = SecuredAction.async(parse.json[RuleConfig]) { request =>
    Future {
      Logger.info(s"Querying incidents: id=$id")

      // TODO check user access to entity (team,user,service)

      val filter = MongoDBObject("_id" -> id)
      val sort = MongoDBObject("name" -> 1)

      val list = Incident.find(filter).sort(sort).toList

      Ok(Json.toJson(list))
    }
  }
}