package resources.rules

import models._
import models.core.ObservedMeasurement

case class RuleParams(threshold:Double, op:String)

class WindowedThresholdFactory extends AkkaStreamsRuleFactory[RuleParams] {
  validateParams { params =>
    params.op match {
      case "gt" | "lt" | "eq" => true
      case _ => false
    }
  }

  buildRule { (config, params, source) =>

    val threshold : Double = params.threshold

    val compare : Double => Boolean = params.op match {
      case "gt" => _ > threshold
      case "lt" => _ < threshold
      case "eq" => _ == threshold
    }

    source.buffer()

    // for each group
    // 1 - drop while !threshold
    // 2 -

    // maybe change Rule to Monitor? fit with datadog naming

    // http://docs.datadoghq.com/guides/monitoring/
    // probably want to follow the datadog model
    //  - on average
    //  - at least once
    //  - at all times
    //  - in total

    // low pass filter
    // if 80% above threshold, then alert

    source.map { om:ObservedMeasurement =>
      if (compare(om.value)) {
        Some(trigger(config, om))
      } else {
        None
      }
    }
  }
}
