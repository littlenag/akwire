package models.notificationvm

import InstructionSet.Instruction
import models.{Incident, notificationvm}
import org.bson.types.ObjectId

case class Program(instructions : List[Instruction]) {

  def instance(incident : Incident) : notificationvm.Process = new notificationvm.Process(ObjectId.get(), this, incident)

  override def toString = instructions.toString()
}
