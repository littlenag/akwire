import akka.actor.ActorRef
import com.mongodb.casbah.commons.conversions.scala._
import models._
import modules.{Init, CoreModule}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.GlobalSettings
import play.api.Application
import play.api.Logger
import plugins.auth.AuthServicePlugin
import scaldi.play.ScaldiSupport
import securesocial.core.{RuntimeEnvironment, AuthenticationMethod, BasicProfile}
import securesocial.core.providers.utils.{PasswordHasher}

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

    // Once everything is setup, now for any initialization
    val incidentEngine = inject[ActorRef] ('incidentEngine)
    val notificationEngine = inject[ActorRef] ('notificationEngine)

    notificationEngine ! Init
    incidentEngine ! Init

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
        adminTeam = Team(Team.AKWIRE_ADMIN_TEAM_NAME)
        Team.save(adminTeam)
      case Some(t) =>
        adminTeam = t
    }

    Logger.info("Teams init complete")

    val hasher = inject[PasswordHasher]

    User.findByEmailAndProvider(User.AKWIRE_ADMIN_USERNAME, User.AKWIRE_ADMIN_PROVIDER) match {
      case None =>

        val id = ObjectId.get()

        val profile = new BasicProfile(
          User.AKWIRE_ADMIN_PROVIDER,
          User.AKWIRE_ADMIN_USERNAME,
          None,           // firstname
          None,           // lastname
          Some("admin"),  // fullname
          Some(User.AKWIRE_ADMIN_USERNAME),
          None,
          AuthenticationMethod.UserPassword,
          None, None,
          Some(hasher.hash(User.AKWIRE_ADMIN_PASSWORD)))

        val tr = new TeamRef(adminTeam.id, adminTeam.name)

        val admin = new User(id, profile, ContactInfo(None), List(tr))

        User.save(admin)

        val defaultPolicy = Policy(ObjectId.get(), OwningEntityRef(admin.id, Scope.USER), "email me", default = true)

        Policy.save(defaultPolicy)

      case _ => Logger.info("User accounts init complete")
    }
  }

  // Load the self-health engine

  // Ensure that we're using GMT, makes Joda happy
  //TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  //DateTimeZone.setDefault(DateTimeZone.UTC);
}
