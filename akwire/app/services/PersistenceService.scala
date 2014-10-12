package services

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import models.core.ObservedMeasurement
import models.Incident
import models.alert.{DoTrigger, AlertMsg}
import org.slf4j.{Logger, LoggerFactory}

class PersistenceService {

  private final val Logger: Logger = LoggerFactory.getLogger(getClass)

  private def genIncidentQuery(alert: AlertMsg) : DBObject = {
    val query = MongoDBObject(
      "active" -> true,
      "resolved" -> false,
      "interred" -> false,
      "rule._id" -> alert.rule.id,
      "incident_key" -> alert.contextualizedStream.asDBObject
    )

    return query;
  }

  def init = {
    Logger.info("Persistence Services Stopping")
  }

  def shutdown = {
    Logger.info("Persistence Services Stopping")
  }

  def persistAlert(alert : DoTrigger) = {
    Logger.info("Normal Alert: {}", alert);

    val q = genIncidentQuery(alert)

    Logger.info("Looking for incidents matching query: {}", q);

    Incident.findOne(q) match {
      case Some(i) =>
        // TODO Error once we have DoProlong in place
        val next_i = i.increment
        Logger.info("Updating incident to: {}", next_i)
        Incident.save(next_i)
        //integrationService.incidentProlong(next_i);
      case None =>
        val i = Incident.fromAlert(alert);
        Logger.info("Saving new incident: {}", i);
        Incident.insert(i);
        //integrationService.incidentTriggered(i);
    }
  }

  def persistMeasurement(obs : ObservedMeasurement) = {

  }

}
