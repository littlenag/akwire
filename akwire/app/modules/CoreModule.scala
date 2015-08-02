package modules

import akka.actor.ActorSystem
import controllers.Policies
import engines.{NotificationEngine, RoutingEngine}
import models.User
import play.api.Logger
import play.api.Configuration
import plugins.auth.AuthServicePlugin
import scaldi.Module
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.{RuntimeEnvironment}
import services._

import play.api.Play.current

import scala.collection.immutable.ListMap

class CoreModule extends Module {
  Logger.debug("DI Start")

  // binding to "foo" <= IS EQUIVALENT TO => bind[String] to "foo"

  import conf.AkkaContext._
  import com.typesafe.config._

  import java.lang.reflect.Constructor

  implicit val env = new RuntimeEnvironment.Default[User] {
    override lazy val userService: AuthServicePlugin = new AuthServicePlugin()

    override lazy val providers : scala.collection.immutable.ListMap[String, securesocial.core.IdentityProvider] = ListMap(
      include(new UsernamePasswordProvider(userService, avatarService, viewTemplates, passwordHashers))
    )
  }

  bind[ActorSystem] toNonLazy ActorSystem("Akwire") destroyWith (_.shutdown())
  bind[Configuration] toNonLazy new play.api.Configuration(ConfigFactory.load("conf/application.conf"))

  // Used by the Global object during first boot
  binding to env
  binding to env.currentHasher

  // Create our REST controllers
  binding to new controllers.Application
  binding to new controllers.Agents
  binding to new controllers.Detectors
  binding to new controllers.Ingest
  binding to new controllers.Roles
  binding to new controllers.Teams
  binding to new controllers.Users
  binding to new controllers.Policies
  binding to new controllers.Auth

  binding to getSSController(classOf[securesocial.controllers.ProviderController])
  binding to getSSController(classOf[securesocial.controllers.LoginApi])

  // The app classloader is our default classloader
  binding to current.classloader

  // Engines (active)
  binding toProvider new PersistenceEngine
  binding toProvider new NotificationEngine
  binding toProvider new RoutingEngine

  // Services (passive)
  binding toNonLazy new IngestService initWith(_.init)
  binding toNonLazy new AlertingService initWith(_.init) destroyWith(_.shutdown)
  binding toNonLazy new PersistenceService initWith(_.init)
  binding toNonLazy new CoreServices initWith(_.init)

  bind[UUIDGenerator] toNonLazy new SimpleUUIDGenerator

  Logger.debug("DI Complete")

  def getSSController[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[User]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(env)
    }
    instance.get
  }
}
