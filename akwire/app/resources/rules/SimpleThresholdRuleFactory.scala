package resources.rules

import akka.stream.scaladsl.Source
import models._
import models.core.ObservedMeasurement

/*

// params
// preferences, settings, config

// checkout DynamicVariable and spray-testkit

// Dimensions:
//  - stream tech: akka | esper
//  - data observed: meas | reports | events | alerts | anomalies

// config.streamExpr.matches(obs)

// Flow of selected obs
// if op(obs, threshold) then trigger
// then resolveWhen { obs => !op(obs,threshold) }

// some way to group, window, etc

ForMeasurements { om:ObservedMeasurement =>
  compare(om.value)
} triggerAndResolveWhen { om:ObservedMeasurement =>
  !compare(om.value)
}
*/

case class RuleParams(threshold:Double, op:String)

class SimpleThresholdRuleFactory extends AkkaStreamsRuleFactory[RuleParams] {

  validateParams { params =>
    params.op match {
      case "gt" | "lt" | "eq" => true
      case _ => false
    }
  }

  buildRule { (config: PersistedRuleConfiguration, params: RuleParams, source: Source[ObservedMeasurement, Unit]) =>

    val threshold : Double = params.threshold

    val compare : Double => Boolean = params.op match {
      case "gt" => _ > threshold
      case "lt" => _ < threshold
      case "eq" => _ == threshold
    }

    source.groupBy(om => (om.instance, om.host, om.observer, om.key)).groupedWithin()

    source.map { om =>
      if (compare(om.value)) {
        Some(trigger(config, om))
      } else {
        None
      }
    }
  }
}
