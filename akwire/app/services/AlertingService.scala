package services

import java.nio.charset.{Charset}
import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import engines.RoutingEngine
import models.alert.{DoTrigger}
import models.core.Observation
import models.{Contextualized, Team, Rule}
import org.bson.types.ObjectId
import scaldi.akka.AkkaInjectable
import scaldi.{Injector}
import scala.collection.concurrent.TrieMap

import play.api.Logger

class AlertingService(implicit inj: Injector) extends AkkaInjectable {

  val classloader = inject[ClassLoader]

  implicit val actorSystem = inject[ActorSystem]

  val dataRouter = injectActorRef[RoutingEngine]

  // rules and their compiled processors mapped via the rule id
  var alertingRules = TrieMap[ObjectId, Rule]()

  // TODO rules and their rules, the rules need to know their StreamContext!!!
  var resolvingRules = TrieMap[ObjectId, List[Rule]]()

  def readFile(path: String, encoding: Charset) : String = {
    val encoded = Files.readAllBytes(Paths.get(path));
    new String(encoded, encoding);
  }

  def init = {
    Logger.info("Alerting Service starting")

    Logger.info("Alerting Services running")
  }

  def shutdown = {
    Logger.info("Alerting Services stopping")
    alertingRules.keys.map(unloadAlertingRule(_))
  }

  // TODO this should be a blocking call to provide back pressure
  def inspect(obs:Observation): Unit = {
    Logger.info(s"Inspecting: $obs")
    //for (ar <- alertingRules) {
    //  ar._2.invoke(obs)
    //}
  }

  def loadAlertingRule(team:Team, rule:Rule) = {

    alertingRules.get(rule.id) match {
      case Some(loadedRule) =>
        Logger.info("Updating rule, rule body: ")

      case None =>

        Logger.info("New rule, compiling complete rule body: ")

        alertingRules.put(rule.id, rule)
    }
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
  def unloadAlertingRule(ruleId:ObjectId) : Option[Rule] = {
    val rule = alertingRules.get(ruleId)

    Logger.info(s"Unloading rule: ${rule}")

    for (resolvingRule <- resolvingRules.get(ruleId).getOrElse(List.empty[Rule])) {
      destroyRule(resolvingRule.id)
      // TODO send an InterAlert message for any loaded Rules
      //persistenceEngine ! DoInter(rule, entry)
    }

    rule.map(r => destroyRule(r.id))

    alertingRules = alertingRules - ruleId
    rule
  }

  /** Go through a java ArrayList since Java is our glue here */
  def triggerAlert(ruleId:ObjectId, obs:java.util.ArrayList[Observation]) = {
    println(s"$ruleId Triggering alert with observations: $obs")
    Logger.info("Triggering alert with observations: " + obs)

    import scala.collection.JavaConverters._

    alertingRules.get(ruleId) match {
      case Some(rule) =>
        dataRouter ! DoTrigger(rule, obs.asScala.toList)
      case None =>
        // Should never happen
        throw new Exception("Should never happen!")
    }
  }

  def resolveAlert(ruleId:ObjectId, obs:java.util.ArrayList[Observation]) = {
    println(s"$ruleId Resolving alert with observations: $obs")
    Logger.info("Resolving alert with observations: " + obs)
  }
}
