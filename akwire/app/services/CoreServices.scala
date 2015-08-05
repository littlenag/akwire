package services

import models.{RuleConfig, Incident}
import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injectable, Injector}

import scala.util.Try

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
    for (rule <- RuleConfig.findAll()) {
      if (rule.active) {
        logger.trace("Loading Alerting Rule: {}", rule)
        alertingEngine.loadAlertingRule(rule)
      } else {
        logger.info("Not Loading Alerting Rule: {}", rule);
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

  def saveRule(rule: RuleConfig) = Try {
    RuleConfig.save(rule)

    // Is the old rule running, if so unload it
    alertingEngine.unloadAlertingRule(rule.id)

    if (rule.active) {
      alertingEngine.loadAlertingRule(rule)
    }

    rule
  }

}


