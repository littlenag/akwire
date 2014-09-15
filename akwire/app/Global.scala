import com.mongodb.casbah.commons.conversions.scala._
import models.Team
import modules.CoreModule
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.GlobalSettings
import play.api.Application
import org.slf4j.{LoggerFactory, Logger}
import scaldi.play.ScaldiSupport

/**
 * Set up the Scaldi injector and provide the mechanism for return objects from the dependency graph.
 */
object Global extends GlobalSettings with ScaldiSupport {

  private final val logger: Logger = LoggerFactory.getLogger("global")

  logger.info("Akwire starting")

  override def applicationModule = {
    new CoreModule
  }

  override def onStart(app: Application) {
    super.onStart(app)
    logger.info("Application has started")
    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()

    firstBoot
  }

  override def onStop(app: Application) {
    super.onStop(app)
    logger.info("Application is stopped")
    DeregisterConversionHelpers()
    DeregisterJodaTimeConversionHelpers()
  }

  def firstBoot = {
    // When akwire starts we need to make sure to have some initial configuration in place.
    // This includes an admin user and an admin team
    Team.findOneByName(Team.AKWIRE_ADMIN_TEAM_NAME) match {
      case None =>
        val admin = new Team(ObjectId.get(), Team.AKWIRE_ADMIN_TEAM_NAME, Nil, new DateTime(), true)
        Team.save(admin)
      case _ => logger.info("Teams init complete")

    }
  }

  // Load the self-health engine

  // Ensure that we're using GMT, makes Joda happy
  //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  //DateTimeZone.setDefault(DateTimeZone.UTC);
}
