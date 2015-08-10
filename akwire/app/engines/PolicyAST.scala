package engines

import models.{Urgency, Impact}
import org.bson.types.ObjectId
import org.joda.time.Duration

import scala.util.Try

trait Target {
  def getEmailAddress : Option[String] = None
  def getPhoneNumber : Option[String] = None
}

object PolicyAST {

  // Javascript only has "numbers" which it then represents as Doubles. We do the same, but call
  // then numbers for consistencies sake.
  type Number = Double

  sealed trait AST {
    // type
    // duration
    //  wait has a definite duration
    //  wait_until has an indefinite duration

    // don't want policies that take more than 24hours to complete, may have to disallow multiple wait_until's in the same path
    // could have a ramp up time that doesn't count...
    // could have it so that you can't wait until after the time specified by your first wait_until
    //   - or wait until's to have be monotonically increasing in all paths and not more than 24h?
    //   - really can't combine wait's and wait_untils

    // global variables / state
    //  any thing in the incident, user, team, service
    //  the SLA, the priority matrix, service owner

    def tipe : Class[_] = throw new RuntimeException("ASTs are untyped unless otherwise specified")
  }

  // Root node for all ASTs; by construction our programs don't repeat
  case class ProgramRoot(block: Block, attempt: Option[Attempts] = None) extends AST

  case class Block(statements: List[AST]) extends AST

  case class ConditionalStatement(cond: AST, ifTrue: AST, ifFalse: AST) extends AST

  case class ActionLiteral(name: String) extends AST

  // count must be > 1, period must be >= longest path through the policy
  case class Attempts(count: Int, period: Option[Duration]) extends AST

  // Generic actions
  case class Page(target: Target) extends AST

  case class Notify(target: Target) extends AST

  // Specific actions, probably want to abstract the method out, more like a function call
  case class Call(target: Target) extends AST

  case class Text(target: Target) extends AST

  case class Email(target: Target) extends AST

  // UI will take care of making is pretty
  case class ThisUser() extends AST with Target            // Self-target for User
  case class User(id: String) extends AST with Target {
    override def getEmailAddress = {
      Try { models.User.findOneById(new ObjectId(id)).flatMap(_.profile.email).get }.toOption
    }
  }

  case class ThisTeam() extends AST with Target            // Self-target for Team
  case class Team(id: String) extends AST with Target

  // Raw Phone number to call or text
  case class PhoneNumber(digits:String) extends AST with Target {
    override def getPhoneNumber = Some(digits)
  }

  case class Wait(duration: Duration) extends AST

  case class Service(id: String) extends AST with Target      // Is this right?

  // Basic Comparison Ops

  case class EqOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe) {
      left.tipe
    } else {
      throw new RuntimeException("Expected equivalent types")
    }
  }

  case class GtOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe && left.tipe == classOf[Number]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Number types")
    }
  }

  case class GteOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe && left.tipe == classOf[Number]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Number types")
    }
  }

  case class LtOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe && left.tipe == classOf[Number]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Number types")
    }
  }

  case class LteOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe && left.tipe == classOf[Number]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Number types")
    }
  }

  case class AndOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe && left.tipe == classOf[Boolean]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Boolean types")
    }
  }

  // a and b
  case class OrOp(left: AST, right: AST) extends AST {
    override val tipe = if (left.tipe == right.tipe && left.tipe == classOf[Boolean]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Boolean types")
    }
  }

  // a or b
  case class NotOp(c: AST) extends AST {
    override val tipe = if (c.tipe == classOf[Boolean]) {
      classOf[Boolean]
    } else {
      throw new RuntimeException("Expected Boolean types")
    }
  }

  // ! a

  // MomentVal?
  //case class TimeRangeVal(value: String) extends AST
  //case class DateRangeVal(value: String) extends AST

  case class ImpactVal(value: Impact.Value) extends AST {
    override val tipe = classOf[Impact.Value]
  }

  case class UrgencyVal(value: Urgency.Value) extends AST {
    override val tipe = classOf[Urgency.Value]
  }

  // P = f(I,U)
  //case class PriorityVal(value: Priority.Value) extends AST

  case class TagVal(value: String) extends AST

  case class TrueVal() extends AST {
    override val tipe = classOf[Boolean]
  }

  case class FalseVal() extends AST {
    override val tipe = classOf[Boolean]
  }

  case class NotExpr(cond: AST) extends AST {
    override val tipe = classOf[Boolean]
  }

  case class NumberVal(value: Number) extends AST {
    override val tipe = classOf[Number]
  }

  case class BooleanVal(value: Boolean) extends AST {
    override val tipe = classOf[Boolean]
  }

  // Representing an empty blocks of statements
  case class Empty() extends AST {
    override val tipe = classOf[Unit]
  }

  case class Property(context: String, field: String) extends AST {
    override val tipe = (context, field) match {
      case ("incident", "impact") => classOf[Impact.Value]
    }
  }

}