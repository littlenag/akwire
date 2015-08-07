package engines

import java.lang.{Process => _}                           // don't want any conflicts with the Java class
import akka.actor.{PoisonPill, Cancellable, Actor}
import models.notificationvm.InstructionSet
import InstructionSet.Instruction
import models.notificationvm.Process
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class ExecuteProcess(process: Process)
case class ProcessCompleted(process: Process)

case object Tick

class ProcessExecutor(implicit inj: Injector) extends Actor with AkkaInjectable {

  override def receive : Receive = waiting

  def waiting: Receive = {
    case ExecuteProcess(process) =>

      // sender is a function, so stash the original sender in a val
      val origSender = sender()

      val listener = new VMStateListener {

        override def instructionStepped(process:Process, instruction: Instruction, newState:Registers, oldState:Registers): Unit = {
          // Save state to the DB.
          Process.save(process.snapshot)
        }

        override def halted(process:Process): Unit = {
          // Save state to the DB.
          Process.save(process.snapshot)
          origSender ! ProcessCompleted(process)
        }

        override def email(process: Process, target: Target) = {
          Logger.info(s"[EMAIL] Notification sent to $target")

          val email = target.getEmailAddress
        }

        override def call(process: Process, target: Target) = {
          Logger.info(s"[CALL] Notification sent to $target")
        }

        override def notify(process: Process, target: Target) = {
          Logger.info(s"[NOTIFY] Notification sent to $target")
        }
      }

      context.become(
        executing(
          process,
          context.system.scheduler.schedule(3 seconds, 3 seconds, self, Tick),
          new VM(listener))
      )
    case  _ => Logger.info("Whoa! already executing something")
  }

  def executing(process: Process, timer:Cancellable, vm:VM): Receive = {
    // Should Tick till completion and nothing more
    case Tick =>
      if (!process.tick()(vm)) {
        // Time to cleanup
        Logger.info(s"Process has terminated: $process")
        self ! PoisonPill
      }
  }
}
