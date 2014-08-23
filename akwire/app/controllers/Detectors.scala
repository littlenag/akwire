package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import play.api.data._
import play.api.libs.json.Reads._
import play.api.libs.json._

import org.bson.types.ObjectId
import org.joda.time.DateTime

import play.api.Play.current
import play.api.PlayException

import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.{MongoConnection}

import models.mongoContext._

import com.mongodb.casbah.MongoConnection
import java.util.UUID

class Detectors extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Detectors])

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models.Detector
  import models.Detector.detectorFormatter
  import com.novus.salat.dao.SalatDAO

  object DetectorsDAO extends SalatDAO[Detector, ObjectId](MongoConnection()("akwire")("detectors"))

  def createDetector = Action.async(parse.json) {
    request =>

      // minLength(3) tupled
      //val customReads: Reads[(String, String)] = (__ \ "name").read[String] and (__ \ "foo").read[String] tupled
      val customReads: Reads[String] = (__ \ "name").read(minLength[String](3))

      customReads.reads(request.body).fold(
        invalid = { errors => Future.successful(BadRequest("invalid json")) },
        valid = { res =>
          val name: String = res
          val detector = new Detector(ObjectId.get(), name, new DateTime(), None, true)
          DetectorsDAO.insert(detector)
          Future.successful(Created(s"Detector Created"))
        }
      )
  }

  def retrieveDetectors = Action.async {
    Future {
      //val filter = MongoDBObject.empty
      val filter = MongoDBObject("active" -> true)
      val sort = MongoDBObject("name" -> 1)
      val list = DetectorsDAO.find(filter).sort(sort).toList
      Ok(Json.arr(list)(0))
    }
  }

  def retrieveDetector(detectorId:String) = Action.async {
    Future {
      val filter = MongoDBObject("_id" -> new ObjectId(detectorId))
      DetectorsDAO.findOne(filter) match {
        case Some(detector : Detector) => Ok(Json.toJson(detector))
        case None => BadRequest(s"Invalid id $detectorId")
      }
    }
  }

  def updateDetector = Action.async(parse.json) {
    request =>
      request.body.asOpt[Detector] match {
        case Some(detector: Detector) =>
          DetectorsDAO.save(detector)
          Future.successful(Ok(Json.toJson(detector)))
        case None =>
          Future.successful(BadRequest(s"Could not parse request body"))
      }
  }

  def deleteDetector = Action.async(parse.json) {
    request =>
      request.body.asOpt[Detector] match {
        case Some(detector: Detector) =>
          DetectorsDAO.removeById(detector._id)
          Future.successful(Ok(s"Removed detector $detector"))
        case None =>
          Future.successful(BadRequest(s"Could not parse request body"))
      }
  }

  def genIntegrationToken(detectorId:String) = Action.async {
    Future {
      val filter = MongoDBObject("_id" -> new ObjectId(detectorId))
      DetectorsDAO.findOne(filter) match {
        case Some(detector : Detector) =>
          val newEntity = detector.copy(integrationToken = Some(UUID.randomUUID()))
          DetectorsDAO.save(newEntity)
          Ok(Json.toJson(newEntity))
        case None => BadRequest(s"Invalid id $detectorId")
      }
    }
  }
}
