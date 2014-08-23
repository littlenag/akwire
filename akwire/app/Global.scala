import com.mongodb.casbah.commons.conversions.scala._
import modules.WebModule
import play.api.GlobalSettings
import play.api.Application
import org.slf4j.{LoggerFactory, Logger}
import scaldi.play.ScaldiSupport

/**
 * Set up the Guice injector and provide the mechanism for return objects from the dependency graph.
 */
object Global extends GlobalSettings with ScaldiSupport {

  private final val logger: Logger = LoggerFactory.getLogger("global")

  logger.info("Akwire starting")

  override def applicationModule = {
    new WebModule
  }

  override def onStart(app: Application) {
    super.onStart(app)
    logger.info("Application has started")
    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()
  }

  override def onStop(app: Application) {
    super.onStop(app)
    logger.info("Application is stopped")
    DeregisterConversionHelpers()
    DeregisterJodaTimeConversionHelpers()
  }

  // Load the self-health engine

  // Ensure that we're using GMT, makes Joda happy
  //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  //DateTimeZone.setDefault(DateTimeZone.UTC);
}
