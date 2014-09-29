package services

import java.nio.charset.{StandardCharsets, Charset}
import java.nio.file.{Files, Paths}
import javax.script.{SimpleScriptContext, ScriptContext}

import models.alert.DoTrigger
import models.core.Observation
import models.{Contextualized, Team, Rule}
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import scaldi.{Injector, Injectable}
import util.ClojureScriptEngineFactory
import util.ClojureScriptEngine
import scala.collection.concurrent.TrieMap

class AlertingEngine(implicit inj: Injector) extends Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AlertingEngine])

  val persist = inject[PersistenceService]

  // rules and their compiled processors mapped via the rule id
  var alertingRules = TrieMap[ObjectId, (Rule, ObsProcesser)]()

  // TODO rules and their rules, the rules need to know their StreamContext!!!
  var resolvingRules = TrieMap[ObjectId, List[Rule]]()

  val clojure = new ClojureScriptEngineFactory().getScriptEngine.asInstanceOf[ClojureScriptEngine]

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

    bindings.put("akwire-bindings/alert-engine", this)

    clojure.getContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

    //val text = io.Source.fromInputStream(getClass.getResourceAsStream("Time.clj")).mkString

    clojure.eval(readFile("/home/mark/proj/akwire/akwire/conf/Time.clj", StandardCharsets.UTF_8), clojure.getContext)
    clojure.eval(readFile("/home/mark/proj/akwire/akwire/conf/Streams.clj", StandardCharsets.UTF_8), clojure.getContext)
//    clojure.eval(io.Source.fromInputStream(getClass.getResourceAsStream("Time.clj")).mkString, clojure.getContext)
//    clojure.eval(readFile("classpath://Streams.clj", StandardCharsets.UTF_8), clojure.getContext)

    logger.info("Alerting Engine running")
  }

  // TODO this should be a blocking call to provide back pressure
  def inspect(obs:Observation): Unit = {
    logger.info(s"Inspecting: $obs")
    for (ar <- alertingRules) {
      ar._2._2.asInstanceOf[ObsProcesser].process(obs)
    }
  }

  def loadAlertingRule(team:Team, rule:Rule) = {
    val ruleName = s"rules.ID_${rule.id}"

    val ruleText = s"""(ns $ruleName (:import services.ObsProcesser) (:require akwire.streams) (:use akwire.streams))
      | (def rule-id (org.bson.types.ObjectId. "${rule.id}"))
      | (defn trigger [events]
      |   (if (list? events)
      |     (.triggerAlert akwire-bindings/alert-engine rule-id (java.util.ArrayList. (map make-obs events)))
      |     (.triggerAlert akwire-bindings/alert-engine rule-id (java.util.ArrayList. (map make-obs [events])))
      |   )
      | )
      |
      | (def ObsProcesserImpl
      |    ( proxy[ObsProcesser][]
      |      (process [observation]
      |        (apply (partial ${rule.text}) [(make-event observation)])
      |      )
      |    )
      | )
    """.stripMargin

    logger.info("Compiling rule body: " + ruleText)

    clojure.eval(ruleText)

    val proc : ObsProcesser = clojure.getInterface(ruleName, classOf[ObsProcesser])

    alertingRules.put(rule.id, (rule, proc))
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

  /** Go through a java ArrayList since Java is our glue here */
  def triggerAlert(ruleId:ObjectId, obs:java.util.ArrayList[Observation]) = {
    println(s"$ruleId Triggering alert with observations: $obs")
    logger.info("Triggering alert with observations: " + obs)

    import scala.collection.JavaConverters._

    alertingRules.get(ruleId) match {
      case Some((rule,proc)) =>
        persist.persistAlert(DoTrigger(rule, obs.asScala.toList))
      case None =>
        // Should never happen
        throw new Exception("Should never happen!")
    }
  }

  def resolveAlert(ruleId:ObjectId, obs:java.util.ArrayList[Observation]) = {
    println(s"$ruleId Resolving alert with observations: $obs")
    logger.info("Resolving alert with observations: " + obs)
  }

}

trait ObsProcesser {
  def process(obs:Observation)
}
