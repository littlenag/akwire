package models.core

import org.joda.time.DateTime

/**
 * base concepts: observers, sensors, and observations
 *  -> sensors have some way to interpreting their environment
 *  -> observers can read sensors and turn their data into interpretable observations
 *  -> these observations form a stream when an observer periodically reports them
 */
sealed trait Observation extends Stream {

  /**
   * other properties
   * tags & k/v pairs (e.g. S=foo P=bar C=baz)
   * units as a plain label
   * scale factor (this is more for UI stuff's)
   */

  /*
   * want to have
   *  - a unique instance id,
   *  - a unique cluster id,
   *  - a "cluster" name to abstract over the instances in the cluster
   *
   * the keyword "network" can then be reserved for the fqdn's monitored by a given cluster
   *
   * will want to retain instance and cluster information as part of a streams provenance
   */

  def timestamp : DateTime
  def instance : String    // getInstance of copper that this observation is tied to, MUST BE A STRING SO THAT ESPER IS HAPPY!
  def host : String         // hostname of the device that generated this metric
  def observer : String     // software that detected this observation
  def key : String          // key of this observation

  //private String tags = "os:/linux";  // metadata about either the device or the stream itself

  override def toString: String = {
    String.format("[%s]/%s/%s/%s/%s", timestamp, instance, host, observer, key)
  }
}

case class ObservedMeasurement(instance:String,
                               host:String,
                               observer:String,
                               key:String,
                               value:Double,
                               timestamp: DateTime = new DateTime()) extends Observation {

  override def toString: String = {
    s"${super.toString()}:$value"
  }
}
