package services

import models.core.Observation
import models.{Contextualized, Team, StreamContext, Rule}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}

class AlertingEngine {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AlertingEngine])

  import clojure.lang.RT
  import clojure.lang.Symbol
  import clojure.lang.Var


  private val evaluator    = RT.`var`("clojure.code", "eval")
  private val stringReader = RT.`var`("clojure.core", "read-string")
  private val require      = RT.`var`("clojure.core", "require")

  var alertingRules = Map[ObjectId, Rule]()
  var resolvingRules = Map[ObjectId, List[Rule]]()

  def init = {
    logger.info("Alerting Engine starting")

    // probably load a whole bunch of clojure related stuff here
    // set the class path directory
    // load security policies
    // load streams functions
    // make clojure aware of the observation classes

    // start the runtime
    // allocate a threadpool to the runtime


    logger.info("Alerting Engine running")
  }

  def inspect(obs:Observation): Unit = {
    logger.info(s"Inspecting: $obs")
    //getRuntime().sendEvent(o);
  }

  def loadAlertingRule(team:Team, rule:Rule) = {
    evaluator.invoke(stringReader.invoke(rule.test))
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
