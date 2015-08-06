package engines

// https://gist.github.com/kmizu/1364341
// http://www.staff.science.uu.nl/~dijks106/SSM/instructions.html
// https://golang.org/ref/spec#Expression

import engines.InstructionSet._
import engines.Primitives._
import models.{Urgency, Impact, Incident}
import org.joda.time.{DateTime, Duration}

import scala.collection.immutable.Stack

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
case class Registers(pc : Int = 0, ws : Option[DateTime] = None)

class Process(val program: Program,
              val incident : Incident,
              val initRegisters : Registers = Registers(0, None),
              val initStack : Stack[Value] = Stack.empty[Value],
              val initVariables : Map[String, Value] = Map.empty[String, Value]) {

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

  private var _stack = initStack
  private var _registers = initRegisters
  private var _variables = initVariables     // Just named memory locations

  def popInt() : Int = {
    val (elem, newStack) = _stack.pop2
    _stack = newStack
    elem.asInstanceOf[IntValue].value
  }

  def popStack() : Value = {
    val (elem, newStack) = _stack.pop2
    _stack = newStack
    elem
  }

  def pushStack(value:Value) = {
    _stack = _stack.push(value)
  }

  def updateRegisters(updated:Registers) = _registers = updated

  def registers = _registers

  def getVar(key: String): Value = {
    if (key.startsWith("incident.")) {
      property(key)
    } else {
      _variables.getOrElse(key, throw new Exception("var '%s' not found".format(key)))
    }
  }

  def setVar(key: String, value: Value): Value = {
    _variables += key -> value
    value
  }

  def mapLabel(label:LBL) : Int = labels(label)

  private def property(key: String): Value = {
    key match {
      case "incident.impact" => ImpactValue(incident.impact)
      case _ => throw new Exception(s"Runtime Error: property '$key' not found")
    }
  }

  /**
   * Move the clock one tick forward for the Process
   * @return true if still executing, false if halted
   */
  def tick()(implicit vm: VM) = {
    vm.tick(this)
  }
}

class Program(val instructions : List[Instruction]) {

  def instance(incident : Incident) : Process = new Process(this, incident)

  override def toString = instructions.toString()
}

object Primitives {

  sealed abstract class Value

  case class StringValue(value: String) extends Value {
    override def toString = value
  }

  case class IntValue(value: Int) extends Value {
    override def toString = value.toString
  }

  case class ImpactValue(value: Impact.Value) extends Value {
    override def toString = value.toString
  }

  case class UrgencyValue(value: Urgency.Value) extends Value {
    override def toString = value.toString
  }

//  case class PriorityValue(value: Priority.Value) extends Value {
//    override def toString() = value.toString
//  }

  case class BooleanValue(value: Boolean) extends Value {
    override def toString = value.toString
  }

  case class MiscValue(value: Any) extends Value {
    override def toString = value.toString
  }

  case object UnitValue extends Value {
    override def toString = "unit"
  }
}

trait VMStateListener {
  // Executed after every partial (not atomic!) update to the virtual machine state,
  // i.e. change to the machine state registers, has been executed.
  // This is distinct from being called after every instruction has finished,
  // which is what `completed` captures.
  // !!!!
  //   THIS IS NECESSARY TO SUPPORT INSTRUCTIONS THAT EXECUTE MULTIPLE ACTIONS IN PARALLEL
  //   and allows the listener to make updates or save state when each action completes
  //   so we avoid repeating actions needlessly.
  // !!!!
  def instructionStepped(instruction:Instruction, newState:Registers, oldState:Registers) = {}

  // Executed after every instruction that completes its execution
  def instructionCompleted(instruction:Instruction, newState:Registers, oldState:Registers) = {}

  // Executed before every instruction regardless of completion status
  def preTick(instruction:Instruction, state:Registers) = {}

  // Executed after every instruction regardless of completion status
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

  // Program has halted, VM needs to be reset and fresh Program inserted
  def halted(state:Registers) = {}
}

//case object HALTED extends ((Registers) => Registers)

class VM(listener: VMStateListener, clock : Clock = new StandardClock()) {
  import InstructionSet._

  /**
   * Move the clock one tick forward for the Process
   * @param proc Process to execute
   * @return true if still executing, false if halted
   */
  def tick(proc:Process): Boolean = {
    val instruction = proc.program.instructions(proc.registers.pc)
    val registers = proc.registers

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
        proc.pushStack(value)
        NEXT

      case ST_VAR(variable) =>
        val value = proc.popStack()
        proc.setVar(variable, value)
        NEXT

      case LD_VAR(variable) =>
        val value = proc.getVar(variable)
        proc.pushStack(value)
        NEXT

      case ADD() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(IntValue(a + b))
        NEXT

      case SUB() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(IntValue(a - b))
        NEXT

      case MUL() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(IntValue(a * b))
        NEXT

      case DIV() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(IntValue(a / b))
        NEXT

      case GT() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(BooleanValue(a > b))
        NEXT

      case GTE() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(BooleanValue(a >= b))
        NEXT

      case LT() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(BooleanValue(a < b))
        NEXT

      case LTE() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(BooleanValue(a <= b))
        NEXT

      case EQ() =>
        val a = proc.popStack()
        val b = proc.popStack()
        Logger.trace(s"$a == $b")
        proc.pushStack(BooleanValue(a == b))
        NEXT

      case JT(lbl) =>
        if (proc.popStack().asInstanceOf[BooleanValue].value) {
          Some(registers.copy(pc = proc.mapLabel(lbl)))
        } else {
          NEXT
        }

      case JF(lbl) =>
        if (!proc.popStack().asInstanceOf[BooleanValue].value) {
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

        // Stepping is implemented by watching for changes to the registers (not memory!) and
        // makes sense because ONLY registers can have multiple or indeterminent values during their execution,
        // whereas things in memory move "atomically" from one state to the next and never changes within
        // an instruction.
        // Keep in mind that complete Program state is more than just its registers since to fully
        // reconstruct a Program one would also need memory.
        if (newRegisters != registers) {
          listener.instructionStepped(instruction, newRegisters, registers)
        }

        if (newRegisters.pc != registers.pc) {
          listener.instructionCompleted(instruction, newRegisters, registers)
        }

        proc.updateRegisters(newRegisters)
        listener.postTick(instruction, proc.registers)
        // This should return an object, not a bool. Oh well.
        true
      case None =>
        listener.halted(proc.registers)
        false
    }
  }
}


