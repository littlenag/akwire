package controllers

import _root_.akka.actor.ActorSystem
import _root_.play.api.GlobalSettings
import org.specs2.mock.Mockito
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import scaldi.play.ScaldiSupport
import services.{AlertingEngine, CoreServices, SimpleUUIDGenerator, UUIDGenerator}
import java.util.UUID

import scaldi._

/**
 * We focus here on testing the controller only - not the infrastructure in front or behind it. Using dependency
 * injection allows the application controller to become testable. It is conceivable that you might have a unit
 * test for the controller if there is enough logic contained in the method that makes it worth testing - the
 * integration test might offer a more useful test if there is not given that you are then testing that the
 * route is configured properly.
 */
class ApplicationUnitTest extends Specification with Mockito {
  
  "Application" should {

    "get uuid from rest api" in {

      class TestModule extends Module {
        binding to new controllers.Application
        binding to new controllers.Agents
        binding to new controllers.Detectors
        binding to new controllers.Ingest
        binding to new controllers.Roles
        binding to new controllers.Teams
        binding to new controllers.Users

        binding to new AlertingEngine
        binding to new CoreServices

        bind[ActorSystem] to ActorSystem("AkkaScalaSpring")
        //bind[Configuration] to new play.api.Configuration(ConfigFactory.load("conf/application.conf"))
        bind[UUIDGenerator] to new SimpleUUIDGenerator
      }

      object TestGlobal extends GlobalSettings with ScaldiSupport {
        // test module will override `MessageService`
        def applicationModule = new TestModule
      }

      running(FakeApplication(withGlobal = Some(TestGlobal))) {
        val uuid = route(FakeRequest(GET, "/randomUUID")).get

        println(contentAsString(uuid))

        status(uuid) must equalTo(OK)
        contentType(uuid) must beSome.which(_ == "text/html")

        //contentAsString(uuid) must contain ("Test Message")
      }
    }

    "invoke the UUID generator" in {
      val uuidGenerator = mock[UUIDGenerator]
      val application = new controllers.Application()(DynamicModule(
       _.bind[UUIDGenerator] to new SimpleUUIDGenerator
      ))

      uuidGenerator.generate returns UUID.randomUUID()

      application.randomUUID(FakeRequest())

      there was one(uuidGenerator).generate
    }
  }
}