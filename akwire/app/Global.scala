import com.mongodb.casbah.commons.conversions.scala._
import models.{TeamRef, User, Team}
import modules.CoreModule
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.GlobalSettings
import play.api.Application
import play.api.Logger
import scaldi.play.ScaldiSupport
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils.{BCryptPasswordHasher, PasswordHasher}
import securesocial.core.{PasswordInfo, AuthenticationMethod}


/**
 * Set up the Scaldi injector and provide the mechanism for return objects from the dependency graph.
 */
object Global extends GlobalSettings with ScaldiSupport {

  Logger.info("Akwire starting")

  override def applicationModule = {
    Logger.info("Defining modules")
    new CoreModule
  }

  override def onStart(app: Application) {
    super.onStart(app)
    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()

    firstBoot
    Logger.debug("onStart() complete")
  }

  override def onStop(app: Application) {
    super.onStop(app)
    DeregisterConversionHelpers()
    DeregisterJodaTimeConversionHelpers()
    Logger.debug("onStop() complete")
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

    Logger.info("Teams init complete")

    User.findByEmailAndProvider(User.AKWIRE_ADMIN_ACCT_NAME, User.AKWIRE_ADMIN_PROVIDER) match {
      case None =>
        //import play.api.Play.current
        val pw = (new BCryptPasswordHasher(play.api.Play.current)).hash("admin")
        val tr = new TeamRef(adminTeam.id, adminTeam.name)
        val admin = new User(ObjectId.get(), User.AKWIRE_ADMIN_ACCT_NAME, User.AKWIRE_ADMIN_PROVIDER, "admin", Some(pw), List(tr))
        User.save(admin)
      case _ => Logger.info("User accounts init complete")
    }
  }

  // Load the self-health engine

  // Ensure that we're using GMT, makes Joda happy
  //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  //DateTimeZone.setDefault(DateTimeZone.UTC);
}
