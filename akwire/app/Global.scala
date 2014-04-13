import akka.actor.ActorSystem
import com.google.inject.{Guice, AbstractModule}
import conf.AppConfiguration
import org.springframework.scala.context.function.FunctionalConfigApplicationContext
import play.api.GlobalSettings
import services.UUIDGenerator
import util.SpringExtentionImpl
import org.slf4j.{LoggerFactory, Logger}

/**
 * Set up the Guice injector and provide the mechanism for return objects from the dependency graph.
 */
object Global extends GlobalSettings {

  /**
   * Bind types such that whenever UUIDGenerator is required, an instance of SimpleUUIDGenerator will be used.
   */
//  val injector = Guice.createInjector(new AbstractModule {
//    protected def configure() {
//      bind(classOf[UUIDGenerator]).to(classOf[SimpleUUIDGenerator])
//    }
//  })

  /**
   * Controllers must be resolved through the application context. There is a special method of GlobalSettings
   * that we can override to resolve a given controller. This resolution is required by the Play router.
   */
  //override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)

  private final val logger: Logger = LoggerFactory.getLogger("global")

  logger.error("Global starting")

  // create a spring context
  implicit val ctx = FunctionalConfigApplicationContext(classOf[AppConfiguration])

  override def getControllerInstance[A](clazz: Class[A]): A = {
    //if (clazz.isAnnotationPresent(Component.class)
    //    || clazz.isAnnotationPresent(Controller.class)
    //|| clazz.isAnnotationPresent(Service.class)
    //|| clazz.isAnnotationPresent(Repository.class))
    return ctx.getBean(clazz);
  }

  import conf.AkkaContext._

  // get hold of the actor system
  val system = ctx.getBean(classOf[ActorSystem])

  //val prop = SpringExtentionImpl(system).props("countingActor")

  // use the Spring Extension to create props for a named actor bean
  //val counter = system.actorOf(prop, "counter")

}
