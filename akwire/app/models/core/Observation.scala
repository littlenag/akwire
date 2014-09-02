package models.core

import org.joda.time.DateTime;

import java.lang.String.format;

/**
 * base concepts: observers, sensors, and observations
 *  -> sensors have some way to interpreting their environment
 *  -> observers can read sensors and turn their data into interpretable observations
 *  -> these observations form a stream when an observer periodically reports them
 */
abstract class Observation(timestamp: DateTime, instance:String, host:String, observer:String, key:String) extends Stream {

  /**
   * other properties
   * tags & k/v pairs (e.g. S=foo P=bar C=baz)
   * units as a plain label
   * scale factor (this is more for UI stuff's)
   */

  //val timestamp = new DateTime();
  //val instance : String    // getInstance of copper that this observation is tied to, MUST BE A STRING SO THAT ESPER IS HAPPY!
  //val host : String         // hostname of the device that generated this metric
  //val observer : String     // software that detected this observation
  //val key : String          // key of this observation

  //private String tags = "os:/linux";  // metadata about either the device or the stream itself

  override def toString() : String = {
    String.format("[%s]/%s/%s/%s/%s", timestamp, instance, host, observer, key);
  }
}

class ObservedMeasurement(timestamp: DateTime,
                          override val instance:String,
                          override val host:String,
                          override val observer:String,
                          override val key:String,
                          value:Double)
      extends Observation(timestamp, instance, host, observer, key) {

  def this(instance : String, host : String, observer: String, key: String, value:Double) {
    this(new DateTime(), instance, host, observer, key, value)
  }

}
