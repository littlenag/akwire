package models

// Urgency measures how quickly a human should address an Incident
object Urgency extends Enumeration {
  type Urgency = Value

  val UL_0 = Value("UL_0")
  val UL_1 = Value("UL_1")
  val UL_2 = Value("UL_2")
  val UL_3 = Value("UL_3")
  val UL_4 = Value("UL_4")
  val UL_5 = Value("UL_5")

  /*
    val NONE = Value("NONE")
    val LOW = Value("LOW")
    val MEDIUM = Value("MEDIUM")
    val HIGH = Value("HIGH")
    val IMMEDIATE = Value("IMMEDIATE")
  */
}