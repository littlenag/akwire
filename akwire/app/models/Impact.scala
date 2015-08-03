package models

// impact is something that the alert knows about itself
// FIXME: CLEAR and INFO are not allowed to have an Urgency attached to them
//  val CLEAR = Value("CLEAR")   // Everything is OK and if anything was wrong in the past its now fixed. Will resolve active situations when received.
//  val INFO = Value("INFO")     // Purely informational in nature, may or may not indicate that anything has gone wrong.
object Impact extends Enumeration {
  type Impact = Value

  val IL_0 = Value("IL_0")
  val IL_1 = Value("IL_1")
  val IL_2 = Value("IL_2")
  val IL_3 = Value("IL_3")
  val IL_4 = Value("IL_4")
  val IL_5 = Value("IL_5")

  // FIXME implement CLEARing logic via an optional filter on the stream of events

  // lots of naming schemes:
  // DEBUG, INFO, WARNING, ERROR, CRITICAL
  // major, minor
  // high, medium, low

  // http://wiki.en.it-processmaps.com/index.php/Checklist_Incident_Priority
}
