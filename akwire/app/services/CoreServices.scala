package services

import com.mongodb.casbah.commons.MongoDBObject
import models.{Incident, Team, Rule}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injectable, Injector}

import scala.util.{Failure, Success, Try}

class CoreServices(implicit inj: Injector) extends Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[CoreServices])

  var alertingEngine = inject[AlertingEngine]

  def init = {
    //loadAdapters()    // to sync alert state with other systems

    // The integration service should probably be loaded first, then the
    // rules loaded. This avoids any race conditions with rules clearing
    // and adapters not being notified.

    // Alerting Rules have to be loaded first since the clearing rules
    // are actually kept in the alerting rule objects as children

    loadAlertingRules
    loadResolveRules

    logger.info("Core Services Running")
  }

  def loadAlertingRules = {
    logger.info("Loading Alerting Rules");
    for (team <- Team.findAll()) {
      logger.info(s"Loading Alerting Rules for Team: ${team.name}")
      for (rule:Rule <- team.rules) {
        if (rule.active) {
          logger.trace("Loading Alerting Rule: {}", rule)
          alertingEngine.loadAlertingRule(team, rule)
        } else {
          logger.info("Not Loading Alerting Rule: {}", rule);
        }
      }
    }
  }

  def loadResolveRules = {
    logger.info("Loading Resolution Rules for Active Situations");
    for (incident <- Incident.findAll()) {
      // Only for "active" situations would we load a resolve rule
      if (incident.active) {
        logger.trace(s"Loading Resolution Rule for 'active' Incident: ${incident}")
        alertingEngine.loadResolveRule(incident.rule.id.get, incident)
      }
    }
  }


  def upsertRule(team:Team, rule:Rule): (Team,Rule) = {
    rule.id match {
      case Some(id) =>
        team.rules.find( v => v.id == rule.id ) match {
          case Some(rule) =>
            // Update to an existing rule
            val updated = team.rules.map( r => if (r.id == rule.id) rule else r)
            val newTeam = team.copy(rules = updated)
            (newTeam, rule)
          case None =>
            // this should be an error
            throw new RuntimeException("thought this was checked ?!?")
        }
      case None =>
        // New Rule, have to assign id for rule
        val newRule = rule.copy(id = Some(new ObjectId()))
        val newTeam = team.copy(rules = team.rules.:+(newRule))
        (newTeam, newRule)
    }
  }

  def saveRule(teamId: ObjectId, rule: Rule): Try[Team] = {
    val teamOpt = Team.findOne(MongoDBObject("_id" -> teamId))

    // Does the team exist?
    if (teamOpt.isEmpty) {
      return Failure(new RuntimeException(s"Invalid team id $teamId"))
    }

    // Team exists, is the rule id valid (either already exists or is None)?
    if (rule.id.isDefined && teamOpt.get.rules.find( r => r.id == rule.id ).isEmpty) {
      return Failure(new RuntimeException(s"Invalid rule id ${rule.id} for team $teamId"))
    }

    val (team,newRule) = upsertRule(teamOpt.get, rule)
    Team.save(team)

    // Is the old rule running, if so unload it
    alertingEngine.unloadAlertingRule(rule.id.get)

    if (newRule.active) {
      alertingEngine.loadAlertingRule(team, newRule)
    }


    return Success(team)
  }
}

