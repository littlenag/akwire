package services

import java.nio.charset.{StandardCharsets, Charset}
import java.nio.file.{Files, Paths}
import javax.script.ScriptContext

import models.alert.DoTrigger
import models.core.Observation
import models.{Contextualized, Team, Rule}
import org.bson.types.ObjectId
import scaldi.{Injector, Injectable}
import util.ClojureScriptEngineFactory
import util.ClojureScriptEngine
import scala.collection.concurrent.TrieMap

import play.api.Logger

import clojure.lang.{Var, RT, IFn, Compiler}

class AlertingEngine(implicit inj: Injector) extends Injectable {

  val persist = inject[PersistenceService]
  val classloader = inject[ClassLoader]

  // rules and their compiled processors mapped via the rule id
  var alertingRules = TrieMap[ObjectId, (Rule, Var)]()

  // TODO rules and their rules, the rules need to know their StreamContext!!!
  var resolvingRules = TrieMap[ObjectId, List[Rule]]()

  val clojure = new ClojureScriptEngineFactory().getScriptEngine.asInstanceOf[ClojureScriptEngine]

  def readFile(path: String, encoding: Charset) : String = {
    val encoded = Files.readAllBytes(Paths.get(path));
    new String(encoded, encoding);
  }

  def init = {
    Logger.info("Alerting Engine starting")

    // probably load a whole bunch of clojure related stuff here
    // set the class path directory
    // load security policies
    // load streams functions
    // make clojure aware of the observation classes

    // start the runtime
    // allocate a threadpool to the runtime

    //require.invoke(stringReader.invoke("clojure"))

    Logger.info("clojure classpath: " + System.getProperty("java.class.path"))

    // FIXME this is a major security hole and will allow arbitrary code execution!
    Compiler.LOADER.bindRoot(classloader)

    val bindings = clojure.createBindings()

    bindings.put("akwire-bindings/alert-engine", this)

    clojure.getContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

    //val text = io.Source.fromInputStream(getClass.getResourceAsStream("Time.clj")).mkString

    clojure.eval(readFile("/home/mark/proj/akwire/akwire/conf/Time.clj", StandardCharsets.UTF_8), clojure.getContext)
    clojure.eval(readFile("/home/mark/proj/akwire/akwire/conf/Streams.clj", StandardCharsets.UTF_8), clojure.getContext)
//    clojure.eval(io.Source.fromInputStream(getClass.getResourceAsStream("Time.clj")).mkString, clojure.getContext)
//    clojure.eval(readFile("classpath://Streams.clj", StandardCharsets.UTF_8), clojure.getContext)
    //Play.application.resourceAsStream("/foo/case0.json")

    Logger.info("Alerting Services running")
  }

  def shutdown = {
    Logger.info("Alerting Services stopping")
    alertingRules.keys.map(unloadAlertingRule(_))
    clojure.eval("(remove-ns akwire)")
  }

  // TODO this should be a blocking call to provide back pressure
  def inspect(obs:Observation): Unit = {
    Logger.info(s"Inspecting: $obs")
    for (ar <- alertingRules) {
      ar._2._2.invoke(obs)
    }
  }

  def loadAlertingRule(team:Team, rule:Rule) = {

    val ruleNS = s"akwire.rules.ID_${rule.id}"

    alertingRules.get(rule.id) match {
      case Some((loadedRule,proc)) =>
        val ruleText = s"""(ns $ruleNS)
          | (def rule-text (partial ${rule.text}))
           """.stripMargin

        Logger.info("Updating rule, rule body: " + ruleText)
        clojure.eval(ruleText)

      case None =>

        val ruleText = s"""
        | (ns $ruleNS
        |   (:require akwire.streams)
        |   (:use akwire.streams))
        | (def rule-id (org.bson.types.ObjectId. "${rule.id}"))
        | (defn trigger [events]
        |   (if (list? events)
        |     (.triggerAlert akwire-bindings/alert-engine rule-id (java.util.ArrayList. (map make-obs events)))
        |     (.triggerAlert akwire-bindings/alert-engine rule-id (java.util.ArrayList. (map make-obs [events])))
        |   )
        | )
        |
        | (def rule-text (partial ${rule.text}))
        |
        | (defn process-observation [observation]
        |   (apply (partial ${rule.text}) [(make-event observation)]))
        |
        | (defn process-event [event]
        |   (apply (partial ${rule.text}) [event]))
        |
        | (defn process [observation]
        |   (apply rule-text [(make-event observation)])
        | )
        |
        """.stripMargin

        Logger.info("New rule, compiling complete rule body: " + ruleText)

        clojure.eval(ruleText)

        //val proc : ObsProcesser = clojure.getInterface(ruleNS, classOf[ObsProcesser])

        val proc = RT.`var`(ruleNS, "process")

        Logger.debug(s"Hook loaded as: ${ruleNS}")
 
        alertingRules.put(rule.id, (rule, proc))
    }
  }

  private def destroyRule(ruleId:ObjectId) = {
    val ruleName = s"rules.ID_${ruleId}"

    // Install a default rule that just returns the event
    val ruleText = s"""(ns $ruleName)
          | (def rule-text (fn [e] e))
           """.stripMargin

    Logger.info("Updating rule, rule body: " + ruleText)
    clojure.eval(ruleText)

    // TODO need a real way to release the rules, maybe (ns-unmap (find-ns 'user) 'foo) could help
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

    Logger.info(s"Unloading rule: ${rule}")

    for (resolvingRule <- resolvingRules.get(ruleId).getOrElse(List.empty[Rule])) {
      destroyRule(resolvingRule.id)
      // TODO send an InterAlert message for any loaded Rules
      //alerts.send(MessageBuilder.withPayload(new Inter(rule, entry.getKey())).build());
    }

    destroyRule(rule.id)
    alertingRules = alertingRules - ruleId
    Some(rule)
  }

  /** Go through a java ArrayList since Java is our glue here */
  def triggerAlert(ruleId:ObjectId, obs:java.util.ArrayList[Observation]) = {
    println(s"$ruleId Triggering alert with observations: $obs")
    Logger.info("Triggering alert with observations: " + obs)

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
    Logger.info("Resolving alert with observations: " + obs)
  }

}
