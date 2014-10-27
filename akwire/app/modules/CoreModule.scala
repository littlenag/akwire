package modules

import akka.actor.ActorSystem
import engines.RoutingEngine
import models.User
import models.alert.AlertMsg
import play.api.Logger
import play.api.Configuration
import plugins.auth.AuthServicePlugin
import scaldi.Module
import securesocial.controllers.ViewTemplates
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils.{PasswordValidator, PasswordHasher}
import securesocial.core.services.AvatarService
import securesocial.core.{BasicProfile, RuntimeEnvironment}
import services._

import play.api.Play.current

import scala.collection.immutable.ListMap

class CoreModule extends Module {
  Logger.debug("Binding dependencies")

  // binding to "foo" <= IS EQUIVALENT TO => bind[String] to "foo"

  import conf.AkkaContext._
  import com.typesafe.config._

  import java.lang.reflect.Constructor

  implicit val env = new RuntimeEnvironment.Default[User] {
//    override lazy val currentHasher: PasswordHasher = new PasswordHasher.Default()
//    override lazy val passwordHashers: Map[String, PasswordHasher] = Map(currentHasher.id -> currentHasher)

    //override lazy val routes = new CustomRoutesService()
    override lazy val userService: AuthServicePlugin = new AuthServicePlugin()

    //override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this)

    //override lazy val avatarService: Option[AvatarService] = None

    //override lazy val eventListeners = List(new MyEventListener())
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
  binding to new controllers.Auth

  binding to getSSController(classOf[securesocial.controllers.ProviderController])
  binding to getSSController(classOf[securesocial.controllers.LoginApi])

  // The app classloader is our default classloader
  binding to current.classloader

  // Engines (active)
  binding toProvider new PersistenceEngine
  binding toProvider new RoutingEngine

  // Services (passive)
  binding toNonLazy new IngestService initWith(_.init)
  binding toNonLazy new AlertingService initWith(_.init) destroyWith(_.shutdown)
  binding toNonLazy new PersistenceService initWith(_.init)
  binding toNonLazy new CoreServices initWith(_.init)

  bind[UUIDGenerator] toNonLazy new SimpleUUIDGenerator

  Logger.debug("Binding complete")

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
