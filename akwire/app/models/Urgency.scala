package models

// Urgency measures how quickly a human should address an Incident
object Urgency extends Enumeration {
  type Urgency = Value

  val UL_0 = Value("UL-0")
  val UL_1 = Value("UL-1")
  val UL_2 = Value("UL-2")
  val UL_3 = Value("UL-3")
  val UL_4 = Value("UL-4")
  val UL_5 = Value("UL-5")

  /*
    val NONE = Value("NONE")
    val LOW = Value("LOW")
    val MEDIUM = Value("MEDIUM")
    val HIGH = Value("HIGH")
    val IMMEDIATE = Value("IMMEDIATE")
  */
}