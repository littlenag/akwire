package controllers

import org.bson.types.ObjectId
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import play.api.libs.json.Reads._
import play.api.libs.json._


/**
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class JsonParsers extends Specification {

  "JsonParsers" should {

    "parse a rule" in {
      import models.Rule

      val ruleText =
        """{"name":"test 1",
          | "text":"(where (and (host \"h1\") (> value 2)) trigger)",
          | "active":true,
          | "team":"5415d1fbec2e527a31f97fe1",
          | "impact":"SEV_5",
          | "urgency":"NONE",
          | "context": ["instance", "host", "observer", "key"]}""".stripMargin

      Json.parse(ruleText).validate[Rule].fold(
        invalid = (e => e mustNotEqual null),
        valid = ( rule => {
          rule.name must_== "test 1"
          rule.active must_== true;
          rule.team must_==(new ObjectId("5415d1fbec2e527a31f97fe1"))
        }) )

    }
  }
}