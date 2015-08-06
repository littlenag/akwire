package services

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem}
import models.alert.{DoResolve, DoTrigger}
import models.core.Observation
import models._
import org.bson.types.ObjectId
import scaldi.akka.AkkaInjectable
import scaldi.Injector
import scala.collection.concurrent.TrieMap

import play.api.Logger

import scala.collection.mutable

trait AlertContext {
  def triggerAlert(rule:TriggeringRule, obs:List[Observation])
  def resolveAlert(rule:ResolvingRule, obs:List[Observation])
}

class AlertingService(implicit inj: Injector) extends AkkaInjectable with AlertContext {

  implicit val actorSystem = inject[ActorSystem]

  val incidentEngine = inject[ActorRef] ('incidentEngine)

  // TODO alerting rules and resolution rules need to know their StreamContext

  // rules and their compiled processors mapped via the rule id
  val builders = mutable.HashSet[RuleBuilder]()

  // rules and their compiled processors mapped via the rule id
  var alertingRules = TrieMap[ObjectId, TriggeringRule]()

  // AlertingRule Id -> child Alerting Rules
  var resolutionRules = TrieMap[ObjectId, List[ResolvingRule]]()

  var obsStream : Stream[Observation] = Stream.Empty

  def readFile(path: String, encoding: Charset) : String = {
    val encoded = Files.readAllBytes(Paths.get(path))
    new String(encoded, encoding)
  }

  def init = {
    Logger.info("Alerting Service starting")

    Logger.info("Alerting Services running")
  }

  def shutdown = {
    Logger.info("Alerting Services stopping")
    alertingRules.keys.map(unloadAlertingRule)
  }

  // TODO this should be a blocking call to provide back pressure
  def inspect(obs:Observation): Unit = {
    Logger.info(s"Inspecting: $obs")
    for ((id, rule) <- alertingRules) {
      rule.inspect(obs)
    }
  }

  def getBuilder(rule:RuleConfig) : RuleBuilder = {
    builders.find(builder => rule.builderClass.instantiates(builder)) match {
      case Some(builder) => builder
      case None =>
        val newBuilder = rule.builderClass.newInstance(this)
        builders += newBuilder
        newBuilder
    }
  }

  def loadAlertingRule(ruleConfig:RuleConfig) = {

    val builder = getBuilder(ruleConfig)

    alertingRules.get(ruleConfig.id) match {
      case Some(loadedRule) =>
        Logger.info("Updating rule, rule body: ")
        loadedRule.unload()
      case None =>
        Logger.info("New rule, compiling complete rule body: ")
    }

    alertingRules.put(ruleConfig.id, builder.buildRule(ruleConfig))
  }

  private def destroyRule(ruleId:ObjectId) = {
    Logger.info("Updating rule, rule body: ")
  }

  // Assumes that the Alerting Rule has already been loaded
  def loadResolveRule(ruleId:ObjectId, entity:Contextualized) = {
    val sc = entity.contextualizedStream
  }

  /**
   * @param ruleId Id of the rule to unload
   * @return The rule that was running
   */
  def unloadAlertingRule(ruleId:ObjectId) : Option[RuleConfig] = {
    val rule = alertingRules.get(ruleId)

    Logger.info(s"Unloading rule: ${rule}")

    for (resolvingRule <- resolutionRules.getOrElse(ruleId, Nil)) {
      destroyRule(resolvingRule.ruleConfig.id)
      // TODO send an InterAlert message for any loaded Rules
      //incidentEngine ! DoInter(rule, entry)
    }

    //rule.map(r => destroyRule(r.id))

    alertingRules = alertingRules - ruleId
    rule.map(_.ruleConfig)
  }

  /** Go through a java ArrayList since Java is our glue here */
  def triggerAlert(rule:TriggeringRule, obs:List[Observation]) = {
    Logger.debug("Triggering alert with observations: " + obs)
    incidentEngine ! DoTrigger(rule.ruleConfig, obs)
  }

  def resolveAlert(rule:ResolvingRule, obs:List[Observation]) = {
    Logger.info("Resolving alert with observations: " + obs)
    incidentEngine ! DoResolve(rule.ruleConfig, obs)
  }
}
