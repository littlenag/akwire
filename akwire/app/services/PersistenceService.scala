package services

import com.mongodb.DBObject
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import models.core.Observation
import models.{Incident, mongoContext, ObservedMeasurement}
import models.alert.{DoTrigger, AlertMsg}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}

class PersistenceService {

  private final val logger: Logger = LoggerFactory.getLogger(getClass)

  private def genIncidentQuery(alert: AlertMsg) : DBObject = {
    val query = MongoDBObject(
      "visible" -> true,
      "resolved" -> false,
      "interred" -> false,
      "rule.id" -> alert.rule.id.get,
      "incident_key" -> makeIncidentKey(alert.rule.context, alert.observations(0))
    )

    return query;
  }

  def persistAlert(alert : DoTrigger) = {
    logger.info("Normal Alert: {}", alert);

    Incident.findOne(genIncidentQuery(alert)) match {
      case Some(i) =>
        val next_i = i.increment
        logger.info("Updating incident to: {}", next_i)
        Incident.save(next_i)
      //integrationService.situationTriggered(next_i);
      case None =>
        val sit : Incident = new Incident(alert);
        logger.info("Saving new incident: {}", sit);
        mt.insert(sit);
      //integrationService.situationTriggered(sit);
    }

    if (i == null) {
    } else {
    }
  }

  def persistMeasurement(obs : ObservedMeasurement) = {

  }

}
