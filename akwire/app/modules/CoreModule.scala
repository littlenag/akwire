package modules

import akka.actor.ActorSystem
import models.User
import models.alert.AlertMsg
import play.api.Logger
import play.api.Configuration
import plugins.auth.AuthServicePlugin
import scaldi.Module
import securesocial.core.RuntimeEnvironment
import services._

import play.api.Play.current

class CoreModule extends Module {
  Logger.debug("Binding dependencies")

  // binding to "foo" <= IS EQUIVALENT TO => bind[String] to "foo"

  import conf.AkkaContext._
  import com.typesafe.config._

  implicit val env = new RuntimeEnvironment.Default[User] {
    //override lazy val routes = new CustomRoutesService()
    override lazy val userService: AuthServicePlugin = new AuthServicePlugin()
    //override lazy val eventListeners = List(new MyEventListener())
  }

  binding to new controllers.Application
  binding to new controllers.Agents
  binding to new controllers.Detectors
  binding to new controllers.Ingest
  binding to new controllers.Roles
  binding to new controllers.Teams
  binding to new controllers.Users

  // The app classloader is our default classloader
  binding to current.classloader

  //val alertsQueue = new scala.collection.mutable.Queue[AlertMsg]

  import scala.collection.mutable.Queue

  binding identifiedBy "alertsQueue" toNonLazy new scala.collection.mutable.Queue[AlertMsg]

  binding toNonLazy new IngestService initWith(_.init)
  binding toNonLazy new AlertingService initWith(_.init) destroyWith(_.shutdown)
  binding toNonLazy new PersistenceService initWith(_.init)
  binding toNonLazy new CoreServices initWith(_.init)

  bind[ActorSystem] toNonLazy ActorSystem("AkkaScalaSpring") destroyWith (_.shutdown())
  bind[Configuration] toNonLazy new play.api.Configuration(ConfigFactory.load("conf/application.conf"))
  bind[UUIDGenerator] toNonLazy new SimpleUUIDGenerator

  Logger.debug("Binding complete")
}
