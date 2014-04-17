package conf

import akka.actor.{ActorSystem}
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.{ContextSupport, FunctionalConfiguration}
import util.SpringExtentionImpl

import org.slf4j.{LoggerFactory, Logger}
import reactivemongo.api.MongoDriver

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

  val mongoConnection = bean("mongoConnection") {
    val driver = new MongoDriver
    driver.connection(List("localhost"))
  }

  val akwireDb = bean("akwireDb") {
    import scala.concurrent.ExecutionContext.Implicits.global

    val connection = mongoConnection()
    connection("akwire")
  }

  val configuration = bean("configuration") {
    // This "Play.current.configuration.getString" construct only works from the web context!
    // So we access the configuration directly.

    import com.typesafe.config._
    new play.api.Configuration(ConfigFactory.load("conf/application.conf"))
  }

}