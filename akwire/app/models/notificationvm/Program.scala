package models.notificationvm

import InstructionSet.Instruction
import com.novus.salat.annotations.Ignore
import engines.PolicyCompiler
import models.{Incident, notificationvm}
import org.bson.types.ObjectId
import play.api.Logger

case class Program(source:String) {

  @Ignore val instructions : List[Instruction] = PolicyCompiler.compile(source).right.get
  Logger.info(s"instructions: $instructions")

  def instance(incident : Incident) : notificationvm.Process = new notificationvm.Process(ObjectId.get(), this, incident)

  override def toString = instructions.toString()
}
