package controllers

import javax.inject.{Named, Singleton, Inject}
import services.{Rabbitmq, UUIDGenerator}
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import akka.actor.ActorRef

/**
 * Instead of declaring an object of Application as per the template project, we must declare a class given that
 * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
 * @param uuidGenerator the UUID generator service we wish to receive.
 *
 * @Named("rabbitmq") aamq : ActorRef
 */
@Singleton
@org.springframework.stereotype.Controller
class Application @Inject() (uuidGenerator: UUIDGenerator) extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])

  def index = Action {
    logger.info("Serving index page...")
    Ok(views.html.index())
  }

  def randomUUID = Action {
    logger.info("calling UUIDGenerator...")
    Ok(uuidGenerator.generate.toString)
  }

}
