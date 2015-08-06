package engines

import akka.actor.{Cancellable, Actor}
import engines.InstructionSet.Instruction
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class ExecuteProcess(process: Process)

case object Tick

class ProcessExecutor(implicit inj: Injector) extends Actor with AkkaInjectable {

  val listener = new VMStateListener {

    override def instructionStepped(instruction: Instruction, newState:Registers, oldState:Registers): Unit = {
      // Save state to the DB.
    }
  }


  val clock = new StandardClock

  implicit val vm = new VM(listener, clock)

  override def receive: Receive = {
    case ExecuteProcess(process) =>
      context.become(executing(process, context.system.scheduler.schedule(3 seconds, 3 seconds, self, Tick)))
    case  _ => Logger.info("whoa! already executing something")
  }

  def executing(process: Process, timer:Cancellable): Receive = {
    case Tick =>
      if (!process.tick()) {
        // Time to cleanup
        Logger.info(s"Process has terminated: ${process}")
      }
  }
}
