package engines

// https://gist.github.com/kmizu/1364341
// http://www.staff.science.uu.nl/~dijks106/SSM/instructions.html
// https://golang.org/ref/spec#Expression

import engines.InstructionSet._
import engines.VM._
import models.{Impact, Incident}
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
      case "incident.impact" => MiscValue(incident.impact)
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

object VM {

  sealed abstract class Value

  case class StringValue(value: String) extends Value {
    override def toString() = value
  }

  case class IntValue(value: Int) extends Value {
    override def toString() = value.toString
  }

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
  // Executed after every atomic update to the virtual machine state, i.e. change to the machine state registers, has been executed
  def latch(instruction:Instruction) = {}

  // Executed before every instruction
  def preTick(instruction:Instruction) = {}

  // Executed after every instruction
  def postTick(instruction:Instruction) = {}

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
    val oldRegisters = proc.registers
    val stack = proc.stack

    val NEXT = Some(oldRegisters.copy(pc = oldRegisters.pc + 1))

    listener.preTick(instruction)

    val regUpdate = instruction match {
      case INVOKE(target, directions) => {
        listener.email(target)
        NEXT
      }

      case WAIT(duration) => {
        val now = clock.now()
        oldRegisters.ws match {
          case Some(t) =>
            if (t.plus(duration).isBefore(now)) {
              // We've finished waiting, move PC to the next instruction, and clear the WAIT register
              listener.wait_complete(duration, now)
              Some(oldRegisters.copy(pc = oldRegisters.pc + 1, ws = None))
            } else {
              // Keep waiting
              listener.wait_continue(duration, now)
              Some(oldRegisters)
            }
          case None =>
            // First time called, initialize the register
            listener.wait_start(duration, now)
            Some(oldRegisters.copy(ws = Some(now)))
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
        stack.push(BooleanValue(a == b))
        NEXT

      case JT(lbl) =>
        if (stack.pop().asInstanceOf[BooleanValue].value) {
          Some(oldRegisters.copy(pc = proc.mapLabel(lbl), ws = None))
        } else {
          NEXT
        }

      case JF(lbl) =>
        if (!stack.pop().asInstanceOf[BooleanValue].value) {
          Some(oldRegisters.copy(pc = proc.mapLabel(lbl), ws = None))
        } else {
          NEXT
        }

      case LBL(index) => {
        // Label's get skipped
        NEXT
      }

      case HALT() =>
        // There is no next state once the machine has halted
        None
      case inst =>
        throw new RuntimeException("[vm] unimplemented: " + inst)
    }

    listener.postTick(instruction)

    regUpdate match {
      case Some(updatedRegisters) =>
        if (updatedRegisters != oldRegisters) {
          listener.latch(instruction)
        }
        proc.registers = updatedRegisters
        true
      case None =>
        false
    }
  }
}


