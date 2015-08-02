package services

import com.mongodb.casbah.commons.MongoDBObject
import models.{RuleConfig, Incident, Team}
import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injectable, Injector}

import scala.util.{Failure, Success, Try}

class CoreServices(implicit inj: Injector) extends Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[CoreServices])

  var alertingEngine = inject[AlertingService]

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

  def shutdown = {
    logger.info("Core Services Stopping")
  }

  def loadAlertingRules = {
    logger.info("Loading Alerting Rules");
    for (team <- Team.findAll().map(Team.hydrate)) {
      logger.info(s"Loading Alerting Rules for Team: ${team.name}")
      for (rule:RuleConfig <- team.rules) {
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
        alertingEngine.loadResolveRule(incident.rule.id, incident)
      }
    }
  }

  def upsertRule(team:Team, rule:RuleConfig): (Team,RuleConfig) = {
    team.rules.find( v => v.id == rule.id ) match {
      case Some(rule) =>
        // Update to an existing rule
        val updated = team.rules.map( r => if (r.id == rule.id) rule else r)
        val newTeam = team.copy(rules = updated)
        (newTeam, rule)
      case None =>
        // New Rule, insert the rule
        val newTeam = team.copy(rules = team.rules.:+(rule))
        (newTeam, rule)
    }
  }

  def createRule(rule: RuleConfig): Try[Team] = {
    val teamId = rule.teamId
    val teamOpt = Team.findOne(MongoDBObject("_id" -> teamId))

    // Does the team exist?
    if (teamOpt.isEmpty) {
      return Failure(new RuntimeException(s"Invalid team id $teamId"))
    }

    // Team exists, is the rule id valid (either already exists or is None)?
    if (teamOpt.get.rules.find( r => r.id == rule.id ).isDefined) {
      logger.info(s"Found existing rule with same id")
      return Failure(new RuntimeException(s"Duplicate rule id ${rule.id} for team $teamId"))
    }

    val (team,newRule) = upsertRule(teamOpt.get, rule)
    Team.save(team)

    if (newRule.active) {
      alertingEngine.loadAlertingRule(team, newRule)
    }

    return Success(team)
  }

  def updateRule(rule: RuleConfig): Try[Team] = {
    val teamId = rule.teamId
    val teamOpt = Team.findOne(MongoDBObject("_id" -> teamId))

    // Does the team exist?
    if (teamOpt.isEmpty) {
      return Failure(new RuntimeException(s"Invalid team id $teamId"))
    }

    // Team exists, is the rule id valid (either already exists or is None)?
    if (teamOpt.get.rules.find( r => r.id == rule.id ).isEmpty) {
      logger.info(s"Could not find rule to update")
      return Failure(new RuntimeException(s"Invalid rule id ${rule.id} for team $teamId"))
    }

    val (team,newRule) = upsertRule(teamOpt.get, rule)
    Team.save(team)

    // Is the old rule running, if so unload it
    alertingEngine.unloadAlertingRule(rule.id)

    if (newRule.active) {
      alertingEngine.loadAlertingRule(team, newRule)
    }


    return Success(team)
  }

}


