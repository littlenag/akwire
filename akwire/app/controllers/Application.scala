package controllers

import play.api.libs.json.{ConstraintReads}
import scaldi.{Injector, Injectable}
import services.{UUIDGenerator}
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._

/**
 * Instead of declaring an object of Application as per the template project, we must declare a class given that
 * the application context is going to be responsible for creating it and wiring it up with the UUID generator service.
 * @param inj IuuidGenerator the UUID generator service we wish to receive.
 */
class Application(implicit inj: Injector) extends Controller with ConstraintReads with Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])

  val uuidGenerator = inject[UUIDGenerator]

  def index = Action {
    logger.info("Serving index page...")
    Ok(views.html.index())
  }

  def randomUUID = Action {
    logger.info("calling UUIDGenerator...")
    Ok(uuidGenerator.generate.toString)
  }
}
