package engines

// https://gist.github.com/kmizu/1364341
// http://www.staff.science.uu.nl/~dijks106/SSM/instructions.html
// https://golang.org/ref/spec#Expression

import engines.InstructionSet._
import engines.Primitives._
import models.{Urgency, Impact, Incident}
import org.joda.time.{DateTime, Duration}

import scala.collection.mutable

import play.api.Logger

trait Clock {
  def now(): DateTime
}

class StandardClock extends Clock {
  def now() = DateTime.now()
}

/**
 * Register state for the policy machine
 * @param pc The Program Counter, points to current instruction to execute
 * @param ws Wait Start, used by the WAIT instruction to stash the start time of the interval
 */
case class Registers(val pc : Int = 0, val ws : Option[DateTime] = None)

class Process(val program: Program, val incident : Incident) {

  // map of labels to PC offset for that label
  val labels = collection.mutable.Map[LBL, Int]().empty

  program.instructions.zipWithIndex.foreach {
    case (lbl:LBL, idx) =>
      if (labels.contains(lbl)) {
        throw new RuntimeException("Invalide byte-code -- duplicate label found: " + lbl)
      } else {
        labels += ((lbl, idx))
      }
    case _ =>
  }

  val stack = new mutable.Stack[Value]

  var registers = Registers(0, None)

  // These are really named memory locations
  private val variables = collection.mutable.Map[String, Value]().empty

  def getVar(key: String): Value = {
    if (key.startsWith("incident.")) {
      property(key)
    } else {
      variables.get(key).getOrElse {
        throw new Exception("var '%s' not found".format(key))
      }
    }
  }

  def setVar(key: String, value: Value): Value = {
    variables(key) = value
    value
  }

  def mapLabel(label:LBL) : Int = labels(label)

  private def property(key: String): Value = {
    key match {
      case "incident.impact" => ImpactValue(incident.impact)
      case _ => throw new Exception(s"Runtime Error: property '$key' not found")
    }
  }

  def tick()(implicit vm: VM) = {
    vm.tick(this)
  }
}

class Program(val instructions : List[Instruction]) {
  def instance(incident : Incident) : Process = {
    return new Process(this, incident)
  }

  override def toString() = instructions.toString()
}

object Primitives {

  sealed abstract class Value

  case class StringValue(value: String) extends Value {
    override def toString() = value
  }

  case class IntValue(value: Int) extends Value {
    override def toString() = value.toString
  }

  case class ImpactValue(value: Impact.Value) extends Value {
    override def toString() = value.toString
  }

  case class UrgencyValue(value: Urgency.Value) extends Value {
    override def toString() = value.toString
  }

//  case class PriorityValue(value: Priority.Value) extends Value {
//    override def toString() = value.toString
//  }

  case class BooleanValue(value: Boolean) extends Value {
    override def toString() = value.toString
  }

  case class MiscValue(value: Any) extends Value {
    override def toString() = value.toString
  }

  case object UnitValue extends Value {
    override def toString() = "unit"
  }
}

trait Listener {
  // Executed after every atomic update to the virtual machine state,
  // i.e. change to the machine state registers, has been executed.
  // This is distinct from being called after every instruction has finished,
  // which is what `completed` captures.
  def latchStateChange(instruction:Instruction, newState:Registers, oldState:Registers) = {}

  // Executed after every instruction that completes its execution
  def completed(instruction:Instruction, newState:Registers, oldState:Registers) = {}

  // Executed before every instruction regardless of completion
  def preTick(instruction:Instruction, state:Registers) = {}

  // Executed after every instruction regardless of completion
  def postTick(instruction:Instruction, state:Registers) = {}

  // Target to email
  def email(target: Target) = {}

  // Target to call
  def call(target: Target) = {}

  // Target to notify
  def notify(target: Target) = {}

  // Hooks to tell what state the wait is in.
  def wait_start(duration: Duration, startTime: DateTime) = {}
  def wait_continue(duration: Duration, curTime: DateTime) = {}
  def wait_complete(duration: Duration, endTime: DateTime) = {}
}

class VM(listener: Listener, clock : Clock = new StandardClock()) {
  import InstructionSet._

  /**
   * Move the clock one tick forward for the Process
   * @param proc Process to execute
   * @return true if still executing, false if halted
   */
  def tick(proc:Process): Boolean = {
    val instruction = proc.program.instructions(proc.registers.pc)
    val registers = proc.registers
    val stack = proc.stack

    val NEXT = Some(registers.copy(pc = registers.pc + 1))

    listener.preTick(instruction, registers)

    val nextState = instruction match {
      case INVOKE(target, directions) => {
        listener.email(target)
        NEXT
      }

      case WAIT(duration) => {
        val now = clock.now()
        registers.ws match {
          case Some(t) =>
            if (t.plus(duration).isBefore(now)) {
              // We've finished waiting, move PC to the next instruction, and clear the WAIT register
              listener.wait_complete(duration, now)
              Some(registers.copy(pc = registers.pc + 1, ws = None))
            } else {
              // Keep waiting
              listener.wait_continue(duration, now)
              Some(registers)
            }
          case None =>
            // First time called, initialize the register
            listener.wait_start(duration, now)
            Some(registers.copy(ws = Some(now)))
        }

      }

      case PUSH(value) =>
        stack.push(value)
        NEXT

      case ST_VAR(variable) =>
        val value = stack.pop()
        proc.setVar(variable, value)
        NEXT

      case LD_VAR(variable) =>
        val value = proc.getVar(variable)
        stack.push(value)
        NEXT

      case ADD() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(IntValue(a + b))
        NEXT

      case SUB() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(IntValue(a - b))
        NEXT

      case MUL() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(IntValue(a * b))
        NEXT

      case DIV() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(IntValue(a / b))
        NEXT

      case GT() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(BooleanValue(a > b))
        NEXT

      case GTE() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(BooleanValue(a >= b))
        NEXT

      case LT() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(BooleanValue(a < b))
        NEXT

      case LTE() =>
        val b = stack.pop().asInstanceOf[IntValue].value
        val a = stack.pop().asInstanceOf[IntValue].value
        stack.push(BooleanValue(a <= b))
        NEXT

      case EQ() =>
        val a = stack.pop()
        val b = stack.pop()
        Logger.trace(s"$a == $b")
        stack.push(BooleanValue(a == b))
        NEXT

      case JT(lbl) =>
        if (stack.pop().asInstanceOf[BooleanValue].value) {
          Some(registers.copy(pc = proc.mapLabel(lbl)))
        } else {
          NEXT
        }

      case JF(lbl) =>
        if (!stack.pop().asInstanceOf[BooleanValue].value) {
          Some(registers.copy(pc = proc.mapLabel(lbl)))
        } else {
          NEXT
        }

      case JMP(lbl) =>
        Some(registers.copy(pc = proc.mapLabel(lbl)))

      // Label's get skipped
      case LBL(index) => {
        NEXT
      }

      case HALT() =>
        // There is no next state once the machine has halted
        None
      case inst =>
        throw new RuntimeException("[vm] unimplemented: " + inst)
    }


    nextState match {
      case Some(newRegisters) =>
        if (newRegisters != registers) {
          listener.latchStateChange(instruction, newRegisters, registers)
        }

        if (newRegisters.pc != registers.pc) {
          listener.completed(instruction, newRegisters, registers)
        }

        proc.registers = newRegisters
        listener.postTick(instruction, proc.registers)
        // This should return an object, not a bool. Oh well.
        true
      case None =>
        false
    }
  }
}


