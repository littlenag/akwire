package models.notificationvm

import com.novus.salat.annotations.raw.Persist
import models.notificationvm.InstructionSet.{HALT, Instruction, LBL}
import com.mongodb.casbah.MongoConnection
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import engines.PolicyAST.{Team, User}
import engines.{Target, VM, Registers}
import models.{Scope, Incident}
import org.bson.types.ObjectId

import models.mongoContext._

/**
 * Represents a running Program. With an appropriate VM a Process can actually be executed
 * and a computation performed.
 * @param program
 * @param incident
 * @param registers
 * @param stack
 * @param memory
 */
case class Process(id: ObjectId,                                            // yep its technically a pid
                   program: Program,
                   incident : Incident,
                   registers : Registers = Registers(),
                   stack : List[Any] = Nil,                                 // ick, Stack is deprecated and may not serialize
                   memory : Map[String, Any] = Map.empty[String, Any]) {

  // We record termination as a register state in order to force processing of the HALT instruction
  // Once HALT has been executed then the halted register is flipped and execution can cease
  def terminated = getRegisters.terminated

  def snapshot = Process(id, program, incident, _registers, _stack, _memory)

  def halt = updateRegisters(getRegisters.copy(terminated = true))

  // Internal copies since these change as the VM executes the Process
  private var _stack = stack
  private var _registers = registers
  private var _memory = memory              // Named rather than indexed memory locations makes for easier debugging

  // map of labels to PC offset for that label, recalculated each time we instantiate
  private var _labels = Map.empty[LBL, Int]

  program.instructions.zipWithIndex.foreach {
    case (lbl:LBL, idx) =>
      if (_labels.contains(lbl)) {
        throw new RuntimeException("Invalide byte-code -- duplicate label found: " + lbl)
      } else {
        _labels += ((lbl, idx))
      }
    case _ =>
  }

  def popInt() : Int = {
    popStack().asInstanceOf[Int]
  }

  def popStack() : Any = {
    val elem = _stack.head
    _stack = _stack.tail
    elem
  }

  def pushStack(value:Any) = {
    _stack = value :: _stack
  }

  def updateRegisters(updated:Registers) : Registers = {
    _registers = updated
    _registers
  }

  def getRegisters = _registers

  def getVar(key: String): Any = {
    key match {
      case "incident.impact" => incident.impact
      case "incident.urgency" => incident.urgency

      case "target.thisUser" =>
        if (incident.owner.scope == Scope.USER)
          User(incident.owner.id.toString).asInstanceOf[Target]
        else
          throw new Exception(s"Compiler Error! Should not accept Keyword 'me' in non-User policy")

      case "target.thisTeam" =>
        if (incident.owner.scope == Scope.TEAM)
          Team(incident.owner.id.toString).asInstanceOf[Target]
        else
          throw new Exception(s"Compiler Error! Should not accept Keyword 'crew' in non-Team policy")

      case _ =>
        _memory.getOrElse(key, throw new Exception(s"Runtime Error: var '$key' not found"))
    }
  }

  def setVar(key: String, value: Any): Any = {
    _memory += key -> value
    value
  }

  def labeltoPC(label:LBL) : Int = _labels(label)

  /**
   * Use the iterator to step the Process through the instructions its going to execute.
   * @param vm
   * @return
   */
  def iterator(implicit vm: VM): Iterator[Instruction] = new Iterator[Instruction] {
    def next() = vm.tick(Process.this)
    def hasNext = !terminated
  }
}

object Process extends ProcessDAO

trait ProcessDAO extends ModelCompanion[Process, ObjectId] {
  // Ah, my very own proc table
  def collection = MongoConnection()("akwire")("processes")

  val dao = new SalatDAO[Process, ObjectId](collection) {}

  // Indexes

  // Queries
}
