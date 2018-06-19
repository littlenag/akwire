package models.notificationvm

import org.joda.time.Duration

object InstructionSet {

  sealed trait Instruction

  // Push a literal value
  case class PUSH(value: Any) extends Instruction

  // Pop the top of the stack, discarding the value
  case class POP() extends Instruction

  // Load a named variable from "memory", push its value onto the top of the stack
  case class LOAD(variable: String) extends Instruction

  // Pop the top of the stack, save it in the named memory location
  case class STORE(variable: String) extends Instruction

  // Assumes the top of the stack is a Target. Pops it and sends a message to the target on the selected channel
  case class DELIVER(directions: Handoff.DeliveryDirections) extends Instruction

  case class WAIT(duration: Duration) extends Instruction

  // Pop the top two values, ___ them, push the result
  case class ADD() extends Instruction
  case class SUB() extends Instruction
  case class MUL() extends Instruction
  case class DIV() extends Instruction

  // With stack [ A B rest ], perform operation A < B, push result
  case class LT() extends Instruction

  // With stack [ A B rest ], perform operation A =< B, push result
  case class LTE() extends Instruction

  // With stack [ A B rest ], perform operation A > B, push result
  case class GT() extends Instruction

  // With stack [ A B rest ], perform operation A >= B, push result
  case class GTE() extends Instruction

  // With stack [ A B rest ], perform operation A == B, push result
  case class EQ() extends Instruction

  // Negate - flips a boolean value
  case class NEG() extends Instruction

  // Jump if top-stack value is FALSE
  case class JF(to : LBL) extends Instruction

  // Jump if top-stack value is TRUE
  case class JT(to : LBL) extends Instruction

  // Jump unconditionally, toSkip must be a positive value with the number of instructions forward to jump
  case class JMP(to : LBL) extends Instruction

  case class LBL(label : Int) extends Instruction

  // Stop execution immediately
  case class HALT() extends Instruction
}

object BinaryOpType extends Enumeration {
  type BinaryOpType = Value
  val ADD, SUB, MUL, DIV, GT, GTE, LT, LTE, EQ = Value
}

object UnaryOpType extends Enumeration {
  type UnaryOpType = Value
  val NEG = Value
}

object Handoff {
  sealed abstract class DeliveryDirections

  // No handoff to another policy
  sealed abstract class ConcreteChannel extends DeliveryDirections

  case object CallTarget extends ConcreteChannel
  case object TextTarget extends ConcreteChannel
  case object EmailTarget extends ConcreteChannel
  //case object AssignTarget extends ConcreteChannel                  // avoid notification, just assign the incident to the target
  case class CustomChannel(method:String) extends ConcreteChannel

  // Hand the incident over to the target's default policy
  sealed abstract class LevelOfEffort extends DeliveryDirections

  case object PageTarget extends LevelOfEffort       // most intrusive, force most bothersome method for target
  case object NotifyTarget extends LevelOfEffort     // normal alert, no overrides
  case object TellTarget extends LevelOfEffort       // least intrusive, choose least disturbing method for target

  // there's also things like:
  //  - running a script
  //  - invoking an http api / webhook
  //  - opening a ticket
}

