import com.mongodb.casbah.commons.conversions.scala._
import models.{TeamRef, User, Team}
import modules.CoreModule
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.GlobalSettings
import play.api.Application
import org.slf4j.{LoggerFactory, Logger}
import scaldi.play.ScaldiSupport
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils.{BCryptPasswordHasher, PasswordHasher}
import securesocial.core.{PasswordInfo, AuthenticationMethod}

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

    var adminTeam:Team = null

    Team.findOneByName(Team.AKWIRE_ADMIN_TEAM_NAME) match {
      case None =>
        adminTeam = new Team(ObjectId.get(), Team.AKWIRE_ADMIN_TEAM_NAME, Nil, new DateTime(), true)
        Team.save(adminTeam)
      case Some(t) =>
        adminTeam = t
    }

    logger.info("Teams init complete")

    User.findByEmailAndProvider(User.AKWIRE_ADMIN_ACCT_NAME, User.AKWIRE_ADMIN_PROVIDER) match {
      case None =>
        //import play.api.Play.current
        val pw = (new BCryptPasswordHasher(play.api.Play.current)).hash("admin")
        val tr = new TeamRef(adminTeam.id, adminTeam.name)
        val admin = new User(ObjectId.get(), User.AKWIRE_ADMIN_ACCT_NAME, User.AKWIRE_ADMIN_PROVIDER, "admin", Some(pw), List(tr))
        User.save(admin)
      case _ => logger.info("User accounts init complete")
    }
  }

  // Load the self-health engine

  // Ensure that we're using GMT, makes Joda happy
  //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  //DateTimeZone.setDefault(DateTimeZone.UTC);
}
