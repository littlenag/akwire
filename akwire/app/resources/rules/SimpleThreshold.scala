package resources.rules

import models._
import models.core.{Observation, ObservedMeasurement}
import services.AlertContext

//
class SimpleThreshold(context: AlertContext) extends RuleBuilder {
  override def buildRule(config: RuleConfig): TriggeringRule = new TriggeringRule {

    val threshold : Double = config.params("threshold").toDouble

    val compare : Double => Boolean = config.params("op") match {
      case "gt" => _ > threshold
      case "lt" => _ < threshold
      case "eq" => _ == threshold
      case _ => throw new RuntimeException("invalid op " + compare)
    }

    override def inspect(obs:Observation) = {
      val m = obs.asInstanceOf[ObservedMeasurement]

      if (compare(m.value)) {
        context.triggerAlert(this, List(obs))
      }
    }

    override def unload(): Unit = {}

    override def ruleConfig: RuleConfig = config
  }
}
