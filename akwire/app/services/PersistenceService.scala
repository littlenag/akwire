package services

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import models.core.ObservedMeasurement
import models.Incident
import models.alert.{DoTrigger, AlertMsg}
import org.slf4j.{Logger, LoggerFactory}

class PersistenceService {

  private final val logger: Logger = LoggerFactory.getLogger(getClass)

  private def genIncidentQuery(alert: AlertMsg) : DBObject = {
    val query = MongoDBObject(
      "visible" -> true,
      "resolved" -> false,
      "interred" -> false,
      "rule.id" -> alert.rule.id,
      "incident_key" -> alert.contextualizedStream.asDBObject
    )

    return query;
  }

  def init = {
    logger.info("Persistence Services Stopping")
  }

  def shutdown = {
    logger.info("Persistence Services Stopping")
  }

  def persistAlert(alert : DoTrigger) = {
    logger.info("Normal Alert: {}", alert);

    Incident.findOne(genIncidentQuery(alert)) match {
      case Some(i) =>
        // TODO Error once we have DoProlong in place
        val next_i = i.increment
        logger.info("Updating incident to: {}", next_i)
        Incident.save(next_i)
        //integrationService.incidentProlong(next_i);
      case None =>
        val i = new Incident(alert);
        logger.info("Saving new incident: {}", i);
        Incident.insert(i);
        //integrationService.incidentTriggered(i);
    }
  }

  def persistMeasurement(obs : ObservedMeasurement) = {

  }

}
