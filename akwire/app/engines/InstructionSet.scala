package engines

import engines.Handoff.DeliveryDirections
import engines.VM.Value
import org.joda.time.Duration

object InstructionSet {

  trait Invokation

  sealed abstract class Instruction

  // Push a literal value
  case class PUSH(value: Value) extends Instruction

  // Pop the top of the stack, discarding the value
  case class POP() extends Instruction

  // Load a variable, push its value onto the top of the stack
  case class LD_VAR(variable: String) extends Instruction

  // Pop the top of the stack, save it in the named variable
  case class ST_VAR(variable: String) extends Instruction

  case class INVOKE(target: Target, directions: DeliveryDirections) extends Instruction with Invokation

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
  abstract class DeliveryDirections

  // No handoff to another policy
  abstract class ConcreteChannel extends DeliveryDirections

  case class CallTarget() extends ConcreteChannel
  case class TextTarget() extends ConcreteChannel
  case class EmailTarget() extends ConcreteChannel
  case class AssignTarget() extends ConcreteChannel              // no means, just assign the incident to the target
  case class CustomChannel(method:String) extends ConcreteChannel

  // Hand the incident over to the target's default policy
  abstract class LevelOfEffort extends DeliveryDirections

  case class PageTarget() extends LevelOfEffort       // most intrusive, force most bothersome method for target
  case class NotifyTarget() extends LevelOfEffort     // normal alert, no overrides
  case class TellTarget() extends LevelOfEffort       // least intrusive, choose least disturbing method for target

  // there's also things like:
  //  - running a script
  //  - invoking an http api / webhook
  //  - opening a ticket
}

