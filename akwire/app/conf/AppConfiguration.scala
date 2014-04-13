package conf

import akka.actor.{Props, ActorSystem}
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.{ContextSupport, FunctionalConfiguration}
import util.SpringExtentionImpl

import org.slf4j.{LoggerFactory, Logger}
import services.Rabbitmq

class AppConfiguration extends FunctionalConfiguration with ContextSupport {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AppConfiguration])

  /**
   * Load implicit context
   */
  implicit val ctx = beanFactory.asInstanceOf[ApplicationContext]

  logger.debug("Spring context loading")

  componentScan("controllers","services")

  /**
   * Actor system singleton for this application.
   */
  val actorSystem = bean() {
    val system = ActorSystem("AkkaScalaSpring")
    // initialize the application context in the Akka Spring Extension
    SpringExtentionImpl(system)
    system
  }

  val rabbitmqService = bean("rabbitmq") {
    val system = actorSystem()
    system.actorOf(Props[Rabbitmq])
  }

}