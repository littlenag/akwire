package engines

import org.joda.time.Duration

trait Target

object PolicyAST {


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

  }

  // By default policies don't repeat
  case class Policy(block: Block, attempt: Option[Attempts] = None) extends AST

  case class Block(statements: List[AST]) extends AST

  case class ConditionalStatement(cond: AST, pos: AST, neg: AST) extends AST

  // List of conditions and their statements
  case class ConditionalList(blocks: List[(AST, AST)], neg: AST) extends AST

  case class ActionLiteral(name: String) extends AST

  // Specific actions, probably want to abstract the method out, more like a function call
  case class Call(target: Target) extends AST

  case class Text(target: Target) extends AST

  case class Email(target: Target) extends AST

  case class Wait(duration: Duration) extends AST

  // count must be > 1, period must be >= longest path through the policy
  case class Attempts(count: Int, period: Option[Duration]) extends AST

  // Generic actions
  case class Page(target: Target) extends AST

  case class Notify(target: Target) extends AST

  // UI will take care of making is pretty
  case class User(id: Int) extends AST with Target

  case class Team(id: Int) extends AST with Target

  case class Service(id: Int) extends AST with Target

  // Basic Ops

  case class EqOp(left: AST, right: AST) extends AST

  case class GtOp(left: AST, right: AST) extends AST

  case class GteOp(left: AST, right: AST) extends AST

  case class LtOp(left: AST, right: AST) extends AST

  case class LteOp(left: AST, right: AST) extends AST

  case class AndOp(l: AST, r: AST) extends AST

  // a and b
  case class OrOp(l: AST, r: AST) extends AST

  // a or b
  case class NotOp(c: AST) extends AST

  // ! a

  // MomentVal?
  //case class TimeRangeVal(value: String) extends AST
  //case class DateRangeVal(value: String) extends AST

  case class ImpactLevel(lvl: Int) extends AST
  case class UrgencyLevel(lvl: Int) extends AST

  // P = f(I,U)
  case class PriorityLevel(lvl: Int) extends AST

  case class TagVal(value: String) extends AST

  case class TrueVal() extends AST

  case class FalseVal() extends AST

  case class NotExpr(cond: AST) extends AST

  case class IntVal(value: Int) extends AST

  case class BooleanVal(value: Boolean) extends AST

  case class UnitVal() extends AST

  // Representing an empty blocks of statements
  case class Empty() extends AST

  case class Property(context: String, field: String) extends AST

}