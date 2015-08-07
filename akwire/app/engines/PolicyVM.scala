package engines

// https://gist.github.com/kmizu/1364341
// http://www.staff.science.uu.nl/~dijks106/SSM/instructions.html
// https://golang.org/ref/spec#Expression

import models.notificationvm.InstructionSet
import InstructionSet._
import models._
import org.joda.time.{DateTime, Duration}

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
  def instructionStepped(process: notificationvm.Process, instruction:Instruction, newState:Registers, oldState:Registers) = {}

  // Executed after every instruction that completes its execution
  def instructionCompleted(process: notificationvm.Process, instruction:Instruction, newState:Registers, oldState:Registers) = {}

  // Executed before every instruction regardless of completion status
  def preTick(process: notificationvm.Process, instruction:Instruction, state:Registers) = {}

  // Executed after every instruction regardless of completion status
  def postTick(process: notificationvm.Process, instruction:Instruction, state:Registers) = {}

  // Target to email
  def email(process: notificationvm.Process, target: Target) = {}

  // Target to call
  def call(process: notificationvm.Process, target: Target) = {}

  // Target to notify
  def notify(process: notificationvm.Process, target: Target) = {}

  // Hooks to tell what state the wait is in.
  def wait_start(process: notificationvm.Process, duration: Duration, startTime: DateTime) = {}
  def wait_continue(process: notificationvm.Process, duration: Duration, curTime: DateTime) = {}
  def wait_complete(process: notificationvm.Process, duration: Duration, endTime: DateTime) = {}

  // Program has halted, VM needs to be reset and fresh Program inserted
  def halted(process: notificationvm.Process) = {}
}

//case object HALTED extends ((Registers) => Registers)

class VM(listener: VMStateListener, clock : Clock = new StandardClock()) {
  import InstructionSet._

  /**
   * Move the clock one tick forward for the Process
   * @param proc Process to execute
   * @return true if still executing, false if halted
   */
  def tick(proc:notificationvm.Process): Boolean = {
    val instruction = proc.program.instructions(proc.registers.pc)
    val registers = proc.registers

    val NEXT = Some(registers.copy(pc = registers.pc + 1))

    listener.preTick(proc, instruction, registers)

    val nextState = instruction match {
      case INVOKE(target, directions) => {
        listener.email(proc, target)
        NEXT
      }

      case WAIT(duration) => {
        val now = clock.now()
        registers.ws match {
          case Some(t) =>
            if (t.plus(duration).isBefore(now)) {
              // We've finished waiting, move PC to the next instruction, and clear the WAIT register
              listener.wait_complete(proc, duration, now)
              Some(registers.copy(pc = registers.pc + 1, ws = None))
            } else {
              // Keep waiting
              listener.wait_continue(proc, duration, now)
              Some(registers)
            }
          case None =>
            // First time called, initialize the register
            listener.wait_start(proc, duration, now)
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
        proc.pushStack(a + b)
        NEXT

      case SUB() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a - b)
        NEXT

      case MUL() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a * b)
        NEXT

      case DIV() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a / b)
        NEXT

      case GT() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a > b)
        NEXT

      case GTE() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a >= b)
        NEXT

      case LT() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a < b)
        NEXT

      case LTE() =>
        val b = proc.popInt()
        val a = proc.popInt()
        proc.pushStack(a <= b)
        NEXT

      case EQ() =>
        val a = proc.popStack()
        val b = proc.popStack()
        Logger.trace(s"$a == $b")
        proc.pushStack(a == b)
        NEXT

      case JT(lbl) =>
        if (proc.popStack().asInstanceOf[Boolean]) {
          Some(registers.copy(pc = proc.labeltoPC(lbl)))
        } else {
          NEXT
        }

      case JF(lbl) =>
        if (!proc.popStack().asInstanceOf[Boolean]) {
          Some(registers.copy(pc = proc.labeltoPC(lbl)))
        } else {
          NEXT
        }

      case JMP(lbl) =>
        Some(registers.copy(pc = proc.labeltoPC(lbl)))

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
          listener.instructionStepped(proc, instruction, newRegisters, registers)
        }

        if (newRegisters.pc != registers.pc) {
          listener.instructionCompleted(proc, instruction, newRegisters, registers)
        }

        proc.updateRegisters(newRegisters)
        listener.postTick(proc, instruction, proc.registers)
        // This should return an object, not a bool. Oh well.
        true
      case None =>
        listener.halted(proc)
        false
    }
  }
}


