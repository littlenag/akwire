package services

import models.{Contextualized, Team, StreamContext, Rule}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}

class AlertingEngine {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AlertingEngine])

  var alertingRules = Map[ObjectId, Rule]()
  var resolvingRules = Map[ObjectId, List[Rule]]()

  def loadAlertingRule(team:Team, rule:Rule) = {

  }

  // Assumes that the Alerting Rule has already been loaded
  def loadResolveRule(ruleId:ObjectId, entity:Contextualized) = {
    val sc = entity.streamContext

  }

  /**
   * @param ruleId Id of the rule to unload
   * @return The rule that was running
   */
  def unloadAlertingRule(ruleId:ObjectId) : Option[Rule] = {
    val rule = alertingRules(ruleId)

    if (rule == null) { return None }

    logger.info("Unloading rule: {}", rule);

    for (resolvingRule <- resolvingRules.get(ruleId).getOrElse(List.empty[Rule])) {
      resolvingRule.destroy
      // TODO send an InterAlert message for any loaded Rules
      //alerts.send(MessageBuilder.withPayload(new Inter(rule, entry.getKey())).build());
    }

    rule.destroy
    alertingRules = alertingRules - ruleId
    Some(rule)
  }

}
