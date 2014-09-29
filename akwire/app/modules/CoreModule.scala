package modules

import akka.actor.ActorSystem
import controllers.Application
import org.slf4j.{LoggerFactory, Logger}
import play.api.Configuration
import scaldi.Module
import services._

class CoreModule extends Module {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[CoreModule])

  logger.debug("Binding dependencies")

  // binding to "foo" <= IS EQUIVALENT TO => bind[String] to "foo"

  import conf.AkkaContext._
  import com.typesafe.config._

  binding to new controllers.Application
  binding to new controllers.Agents
  binding to new controllers.Detectors
  binding to new controllers.Ingest
  binding to new controllers.Roles
  binding to new controllers.Teams
  binding to new controllers.Users

  binding to new AlertingEngine initWith(_.init)
  binding to new PersistenceService
  binding to new CoreServices

  bind[ActorSystem] to ActorSystem("AkkaScalaSpring")
  bind[Configuration] to new play.api.Configuration(ConfigFactory.load("conf/application.conf"))
  bind[UUIDGenerator] to new SimpleUUIDGenerator
}
