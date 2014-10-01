package modules

import akka.actor.ActorSystem
import controllers.Application
import play.api.Logger
import play.api.Configuration
import scaldi.Module
import services._

import play.api.Play.current

class CoreModule extends Module {
  Logger.debug("Binding dependencies")

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

  // The app classloader is our default classloader
  binding to current

  binding toNonLazy new IngestService initWith(_.init)
  binding toNonLazy new AlertingEngine initWith(_.init) destroyWith(_.shutdown)
  binding toNonLazy new PersistenceService initWith(_.init)
  binding toNonLazy new CoreServices initWith(_.init)

  bind[ActorSystem] toNonLazy ActorSystem("AkkaScalaSpring")
  bind[Configuration] toNonLazy new play.api.Configuration(ConfigFactory.load("conf/application.conf"))
  bind[UUIDGenerator] toNonLazy new SimpleUUIDGenerator

  Logger.debug("Binding complete")
}
