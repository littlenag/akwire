package modules

import akka.actor.{ActorRef, ActorSystem}
import controllers.Policies
import engines.{IncidentEngine, ProcessExecutor, NotificationEngine}
import models.User
import play.api.Logger
import play.api.Configuration
import plugins.auth.AuthServicePlugin
import scaldi.Module
import scaldi.akka.AkkaInjectable
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
  bind[Configuration] toNonLazy new play.api.Configuration(ConfigFactory.load)

  // Used by the Global object during first boot
  binding to env
  binding to env.currentHasher

  // Create our REST controllers
  binding to new controllers.Application
  binding to new controllers.Agents
  binding to new controllers.Detectors
  binding to new controllers.Ingest
  binding to new controllers.Roles
  binding to new controllers.Rules
  binding to new controllers.Teams
  binding to new controllers.Users
  binding to new controllers.Policies
  binding to new controllers.Incidents
  binding to new controllers.Auth

  binding to getSSController(classOf[securesocial.controllers.ProviderController])
  binding to getSSController(classOf[securesocial.controllers.LoginApi])

  // The app classloader is our default classloader
  binding to current.classloader

  // Careful, injectActorRef[T] creates a new instance of T !!
  binding toProvider new IncidentEngine
  binding toProvider new NotificationEngine

  // Engines (active); FIXME replace these with cluster singleton proxies
  bind[ActorRef] identifiedBy 'incidentEngine to {
    implicit val system = inject[ActorSystem]
    AkkaInjectable.injectActorRef[IncidentEngine]
  }

  bind[ActorRef] identifiedBy 'notificationEngine to {
    implicit val system = inject[ActorSystem]
    AkkaInjectable.injectActorRef[NotificationEngine]
  }

  // Executors
  binding toProvider new ProcessExecutor

  // Services (passive)
  binding toNonLazy new IngestService initWith(_.init)
  binding toNonLazy new AlertingService initWith(_.init) destroyWith(_.shutdown)
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
