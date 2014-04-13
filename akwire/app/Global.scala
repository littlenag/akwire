import com.google.inject.{Guice, AbstractModule}
import play.api.GlobalSettings
import services.{SimpleUUIDGenerator, UUIDGenerator}

/**
 * Set up the Guice injector and provide the mechanism for return objects from the dependency graph.
 */
object Global extends GlobalSettings {

  /**
   * Bind types such that whenever UUIDGenerator is required, an instance of SimpleUUIDGenerator will be used.
   */
  val injector = Guice.createInjector(new AbstractModule {
    protected def configure() {
      bind(classOf[UUIDGenerator]).to(classOf[SimpleUUIDGenerator])
    }
  })

  /**
   * Controllers must be resolved through the application context. There is a special method of GlobalSettings
   * that we can override to resolve a given controller. This resolution is required by the Play router.
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)

    // create a spring context
  implicit val ctx = FunctionalConfigApplicationContext(classOf[AppConfiguration])

  import Config._

  // get hold of the actor system
  val system = ctx.getBean(classOf[ActorSystem])

  val prop = SpringExtentionImpl(system).props("countingActor")

  // use the Spring Extension to create props for a named actor bean
  val counter = system.actorOf(prop, "counter")

  // tell it to count three times
  counter ! COUNT
  counter ! COUNT
  counter ! COUNT

  val result = (counter ? GET).mapTo[Int]
  // print the result
  result onComplete {
    case Success(result) => println(s"Got back $result")
    case Failure(failure) => println(s"Got an exception $failure")
  }

  system.shutdown
  system.awaitTermination
}
