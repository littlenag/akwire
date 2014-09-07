package services

import java.nio.charset.{StandardCharsets, Charset}
import java.nio.file.{Files, Paths}
import javax.script.{SimpleScriptContext, ScriptContext}

import models.core.Observation
import models.{Contextualized, Team, StreamContext, Rule}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import util.ClojureScriptEngineFactory
import util.ClojureScriptEngine
import scala.collection.concurrent.TrieMap

trait ObsProcesser {
  def process(obs:Observation)
}

trait TriggerCallback {
  /** Go through a java ArrayList since Java is our glue here */
  def trigger(obs : java.util.ArrayList[Observation])
}

class TriggerAlert extends TriggerCallback {
  private final val logger: Logger = LoggerFactory.getLogger(getClass)

  def trigger(obs : java.util.ArrayList[Observation]) = {
    println("Triggering alert with observations: " + obs)
    logger.info("Triggering alert with observations: " + obs)
  }
}

class AlertingEngine {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AlertingEngine])

  // rules and their compiled processors mapped via the rule id
  var alertingRules = TrieMap[ObjectId, (Rule, ObsProcesser)]()

  // TODO rules and their rules, the rules need to know their StreamContext!!!
  var resolvingRules = TrieMap[ObjectId, List[Rule]]()

  val clojure = new ClojureScriptEngineFactory().getScriptEngine.asInstanceOf[ClojureScriptEngine]

  val alertCollector = new TriggerAlert

  object Actions {
    def prn(o:Any) = {
      println(o)
    }
  }

  def readFile(path: String, encoding: Charset) : String = {
    val encoded = Files.readAllBytes(Paths.get(path));
    new String(encoded, encoding);
  }

  def init = {
    logger.info("Alerting Engine starting")

    // probably load a whole bunch of clojure related stuff here
    // set the class path directory
    // load security policies
    // load streams functions
    // make clojure aware of the observation classes

    // start the runtime
    // allocate a threadpool to the runtime

    //require.invoke(stringReader.invoke("clojure"))

    val bindings = clojure.createBindings()

    bindings.put("akwire-binding-trigger", alertCollector)

    clojure.getContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

    clojure.eval(readFile("/home/mark/proj/akwire/akwire/app/util/Time.clj", StandardCharsets.UTF_8), clojure.getContext)
    clojure.eval(readFile("/home/mark/proj/akwire/akwire/app/util/Streams.clj", StandardCharsets.UTF_8), clojure.getContext)

    logger.info("Alerting Engine running")
  }

  def inspect(obs:Observation): Unit = {
    logger.info(s"Inspecting: $obs")
    for (ar <- alertingRules) {
      ar._2._2.asInstanceOf[ObsProcesser].process(obs)
    }
  }

  def loadAlertingRule(team:Team, rule:Rule) = {
    val ruleName = s"rules.ID_${rule.id.get}"

    val ruleText = s"""(ns $ruleName (:import services.ObsProcesser) (:require akwire.streams) (:use akwire.streams))
      | (def ObsProcesserImpl
      |    ( proxy[ObsProcesser][]
      |      (process [observation]
      |        (apply (partial ${rule.test}) [(make-event observation)])
      |      )
      |    )
      | )
    """.stripMargin

    //|        (apply (partial ${rule.test}) [(make-event observation)])

    logger.info("Compiling rules: " + ruleText)

    clojure.eval(ruleText)

    val proc : ObsProcesser = clojure.getInterface(ruleName, classOf[ObsProcesser])

    alertingRules.put(rule.id.get, (rule, proc))
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
    val (rule, _) = alertingRules(ruleId)

    if (rule == null) { return None }

    logger.info("Unloading rule: {}", rule);

    for (resolvingRule <- resolvingRules.get(ruleId).getOrElse(List.empty[Rule])) {
      resolvingRule.destroy
      // TODO send an InterAlert message for any loaded Rules
      //alerts.send(MessageBuilder.withPayload(new Inter(rule, entry.getKey())).build());
    }

    //rule.destroy
    alertingRules = alertingRules - ruleId
    Some(rule)
  }

}
