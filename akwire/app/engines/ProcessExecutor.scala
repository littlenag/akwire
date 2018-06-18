package engines

import java.lang.{Process => _}
import java.util

import com.twilio.sdk.TwilioRestClient
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair

// don't want any conflicts with the Java class
import akka.actor.{PoisonPill, Cancellable, Actor}
import models.notificationvm.InstructionSet
import InstructionSet.Instruction
import models.notificationvm.Process
import play.api.Logger
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Play.current

case class ExecuteProcess(process: Process)
case class ProcessCompleted(process: Process)

case object Tick
case object EarlyTermination

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

        override def email(process: Process, target: Target): Unit = {
          Logger.info(s"[EMAIL] Notification sent to $target")

          val email = target.getEmailAddress
        }

        override def text(process: Process, target: Target): Unit = {
          Logger.info(s"[TEXT] Notification sent to $target")

          // Ten digit number enforced by Parser
          val digits = target.getPhoneNumber.get

          val AKWIRE_NUMBER = "+13236724363"
          val accountSid = current.configuration.getString("twilio.accountSID").get
          val authToken = current.configuration.getString("twilio.authToken").get

          val client = new TwilioRestClient(accountSid, authToken)

          // Build a filter for the MessageList
          val params = new util.ArrayList[NameValuePair]()
          params.add(new BasicNameValuePair("Body", s"Problem on ${process.incident.contextualizedStream}"))
          params.add(new BasicNameValuePair("To", "+1"+digits))
          params.add(new BasicNameValuePair("From", AKWIRE_NUMBER))

          val messageFactory = client.getAccount().getMessageFactory()
          val message = messageFactory.create(params)
          Logger.info(message.getSid())
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

    case EarlyTermination =>
      Logger.info("Exiting without having started Proc")
      self ! PoisonPill

    case msg => Logger.info(s"Whoa! already executing something but still got: $msg")
  }

  def executing(process: Process, timer:Cancellable, vm:VM): Receive = {
    // Should Tick till completion and nothing more
    case Tick =>
      Logger.debug("tick")
      val iter = process.iterator(vm)
      if (iter.hasNext) {
        iter.next()
      } else {
        // Time to cleanup
        Logger.info(s"Process has terminated: $process")
        Process.save(process.snapshot)
        self ! PoisonPill
      }
    case EarlyTermination =>
      Logger.info("Exiting early")
      timer.cancel()
      process.halt
      Process.save(process.snapshot)
      self ! PoisonPill
  }
}
