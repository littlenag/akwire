package services

import models.core.{ObservedMeasurement, Observation}
import models.{PersistedRuleConfig$, Team}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable._
import play.api.test.FakeApplication
import scaldi.DynamicModule

import play.api.test._
import play.api.test.Helpers._

class PersistenceTest extends Specification with Mockito {

  "PersistenceEngine" should {

    "read a team document" in {

      running(FakeApplication()) {
        val teams = Team.findAll()

        for (t <- teams) {
          println("Team: " + t)
        }

        teams mustNotEqual null
      }

    }
  }
}