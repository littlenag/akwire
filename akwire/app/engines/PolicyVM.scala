package engines

// https://gist.github.com/kmizu/1364341
// http://www.staff.science.uu.nl/~dijks106/SSM/instructions.html

import engines.InstructionSet._
import engines.VM._
import models.Incident
import org.joda.time.{DateTime, Duration}

import scala.collection.mutable
import scala.util.parsing.combinator.RegexParsers

import play.api.Logger

trait Clock {
  def now(): DateTime
}

class StandardClock extends Clock {
  def now() = DateTime.now()
}

/**
 * Register state for the policy machine
 * @param pc The Program Counter, points to current instruction to execute
 * @param ws Wait Start, used by the WAIT instruction to stash the start time of the interval
 */
case class Registers(val pc : Int = 0, val ws : Option[DateTime] = None)

class Process(val program: Program, val incident : Incident) {

  // map of labels to PC offset for that label
  val labels = collection.mutable.Map[LBL, Int]().empty

  program.instructions.zipWithIndex.foreach {
    case (lbl:LBL, idx) =>
      if (labels.contains(lbl)) {
        throw new RuntimeException("Invalide byte-code -- duplicate label found: " + lbl)
      } else {
        labels += ((lbl, idx))
      }
    case _ =>
  }

  val stack = new mutable.Stack[Value]

  var registers = Registers(0, None)

  // These are really named memory locations
  private val variables = collection.mutable.Map[String, Value]().empty

  def getVar(key: String): Value = {
    variables.get(key).getOrElse {
      throw new Exception("symbol'%s' not found".format(key))
    }
  }

  def setVar(key: String, value: Value): Value = {
    variables(key) = value
    value
  }

  def mapLabel(label:LBL) : Int = labels(label)

  def property(key: String): Value = {
    key match {
      case "incident.severity" => IntValue(incident.impact.id)
      case _ => throw new Exception(s"Runtime Error: property '$key' not found")
    }
  }

  def tick()(implicit vm: VM) = {
    vm.tick(this)
  }
}

class Program(val instructions : List[Instruction]) {
  def instance(incident : Incident) : Process = {
    return new Process(this, incident)
  }

  override def toString() = instructions.toString()
}

object VM {

  sealed abstract class Value

  case class StringValue(value: String) extends Value {
    override def toString() = value
  }

  case class IntValue(value: Int) extends Value {
    override def toString() = value.toString
  }

  case class BooleanValue(value: Boolean) extends Value {
    override def toString() = value.toString
  }

  case object UnitValue extends Value {
    override def toString() = "unit"
  }
}

trait Listener {
  // Executed after every atomic update to the virtual machine state, i.e. change to the machine state registers, has been executed
  def latch(instruction:Instruction) = {}

  // Executed before every instruction
  def preTick(instruction:Instruction) = {}

  // Executed after every instruction
  def postTick(instruction:Instruction) = {}

  // Target to email
  def email(target: Target) = {}

  // Target to call
  def call(target: Target) = {}

  // Target to notify
  def notify(target: Target) = {}

  // Hooks to tell what state the wait is in.
  def wait_start(duration: Duration, startTime: DateTime) = {}
  def wait_continue(duration: Duration, curTime: DateTime) = {}
  def wait_complete(duration: Duration, endTime: DateTime) = {}
}

class VM(listener: Listener, clock : Clock = new StandardClock()) {
  import InstructionSet._

  /**
   * Move the clock one tick forward for the Process
   * @param proc Process to execute
   * @return true if still executing, false if halted
   */
  def tick(proc:Process): Boolean = {
    val instruction = proc.program.instructions(proc.registers.pc)
    val oldRegisters = proc.registers
    val stack = proc.stack

    val NEXT = Some(oldRegisters.copy(pc = oldRegisters.pc + 1))

    listener.preTick(instruction)

    val regUpdate = instruction match {
      case EMAIL(target) => {
        listener.email(target)
        NEXT
      }

      case WAIT(duration) => {
        val now = clock.now()
        oldRegisters.ws match {
          case Some(t) =>
            if (t.plus(duration).isBefore(now)) {
              // We've finished waiting, move PC to the next instruction, and clear the WAIT register
              listener.wait_complete(duration, now)
              Some(oldRegisters.copy(pc = oldRegisters.pc + 1, ws = None))
            } else {
              // Keep waiting
              listener.wait_continue(duration, now)
              Some(oldRegisters)
            }
          case None =>
            // First time called, initialize the register
            listener.wait_start(duration, now)
            Some(oldRegisters.copy(ws = Some(now)))
        }

      }

      case PUSH(value) =>
        stack.push(value)
        NEXT

      case ST_VAR(variable) =>
        val value = stack.pop()
        proc.setVar(variable, value)
        NEXT

      case LD_VAR(variable) =>
        val value = proc.getVar(variable)
        stack.push(value)
        NEXT

      case ADD() =>
        val a = stack.pop().asInstanceOf[IntValue].value
        val b = stack.pop().asInstanceOf[IntValue].value
        stack.push(IntValue(a + b))
        NEXT

      case CMP() =>
        val a = stack.pop().asInstanceOf[IntValue].value
        val b = stack.pop().asInstanceOf[IntValue].value
        val cmp = b.compareTo(a)
        println(s"a $a b $b c $cmp")
        stack.push(IntValue(cmp))
        NEXT

      case JGT(lbl) =>
        val a = stack.pop().asInstanceOf[IntValue].value
        if (a > 0) {
          Some(oldRegisters.copy(pc = proc.mapLabel(lbl), ws = None))
        } else {
          NEXT
        }

      case JLT(lbl) =>
        val a = stack.pop().asInstanceOf[IntValue].value
        if (a < 0) {
          Some(oldRegisters.copy(pc = proc.mapLabel(lbl), ws = None))
        } else {
          NEXT
        }

      case LBL(index) => {
        // Label's get skipped
        NEXT
      }

      case HALT() =>
        // There is no next state once the machine has halted
        None
      case inst =>
        throw new RuntimeException("unimplemented: " + inst)
    }

    listener.postTick(instruction)

    regUpdate match {
      case Some(updatedRegisters) =>
        if (updatedRegisters != oldRegisters) {
          listener.latch(instruction)
        }
        proc.registers = updatedRegisters
        true
      case None =>
        false
    }
  }
}

object Compiler {
  import InstructionSet._

  val VAR_CUR_COUNT = "count"
  val VAR_MAX_REPEAT = "max"

  val parser = new NotificationPolicyParser

  class LabelMaker(val start:Int = 0) {
    var cur = start

    def next() = {
      cur = cur + 1
      LBL(cur)
    }
  }

  def compile(policy: String): Either[parser.NoSuccess, Program] = {
    Logger.info("Compiling policy: " + policy)

    parser.parse(policy) match {
      case parser.Success(result, _) =>

        // And then the Environment will need to point at the next instruction to execute
        implicit val labelMaker = new LabelMaker()
        val instructions = compileAST(result.asInstanceOf[AST])
        Right(new Program(instructions))
      case er: parser.NoSuccess =>
        Logger.error("Parse error: " + er)
        Left(er)
    }
  }

  private def compileAST(ast:AST)(implicit labeler: LabelMaker): List[Instruction] = {
    def visit(ast:AST): List[Instruction] = {
      ast match {
        case Policy(statements, repeat_expr) => {

          // Extract the number of times to loop
          val max = repeat_expr match {
            case Some(Attempts(count, _)) => count
            case None => 0
          }

          // If we repeat, then include those statements
          val preamble = if (max > 0) {
            // -1 since the first time through shouldn't contribute
            List(PUSH(IntValue(-1)), ST_VAR(VAR_CUR_COUNT), PUSH(IntValue(max)), ST_VAR(VAR_MAX_REPEAT))
          } else {
            Nil
          }

          val body = compileAST(statements)

          val counter_inc = if (max > 0) {
            List(LD_VAR(VAR_CUR_COUNT), PUSH(IntValue(1)), ADD(), ST_VAR(VAR_CUR_COUNT))
          } else {
            Nil
          }

          val (repeat_instr, body_start) = repeat_expr match {
            case Some(Attempts(count, Some(period))) =>
              (Nil, Nil)
            case Some(Attempts(count, None)) =>
              val body_start = labeler.next()
              // If the repeat > count then jump back to the top of the loop, which would be just after the preamble
              (List(LD_VAR(VAR_CUR_COUNT), LD_VAR(VAR_MAX_REPEAT), CMP(), JLT(body_start)), List(body_start))
            case None =>
              (Nil, Nil)
          }

          preamble ::: body_start ::: body ::: counter_inc ::: repeat_instr ::: List(HALT())
        }
        case Block(exprs) => {
          exprs.foldLeft(List.empty[Instruction]){(result : List[Instruction], x) => (result ::: (compileAST(x)))}
        }
        case ConditionalStatement(cond, pos, neg) => {
          val cond_expr = compileAST(cond)

          val true_branch = compileAST(pos)

          val false_branch_label = labeler.next()
          val after_label = labeler.next()

          val false_branch = compileAST(pos)

          // The cond branch must result in a boolean value on the top of the stack
          // if the condition is false, jump past the true branch
          cond_expr ::: List(JF(false_branch_label)) :::
          true_branch ::: List(JMP(after_label), false_branch_label) :::
          false_branch :::
          List(after_label)
        }
        case Email(u @ User(name)) => {
          List(EMAIL(u))
        }
        case Wait(duration) => {
          List(WAIT(duration))
        }
        case x => throw new RuntimeException("implement me: " + x)
      }
    }
    visit(ast)
  }
}

object InstructionSet {

  trait Invokation

  sealed abstract class Instruction

  // Push a literal value
  case class PUSH(value: Value) extends Instruction

  // Pop the top of the stack, discarding the value
  case class POP() extends Instruction

  // Load a variable, push its value onto the top of the stack
  case class LD_VAR(variable: String) extends Instruction

  // Pop the top of the stack, save it in the named variable
  case class ST_VAR(variable: String) extends Instruction

  case class PAGE(target: Target) extends Instruction with Invokation
  case class NOTIFY(target: Target) extends Instruction with Invokation

  case class EXEC(policyName: String) extends Instruction with Invokation

  case class CALL(target: Target) extends Instruction with Invokation
  case class EMAIL(target: Target) extends Instruction with Invokation
  case class TEXT(target: Target) extends Instruction  with Invokation

  case class WAIT(duration: Duration) extends Instruction

  // Math OPS
  case class ADD() extends Instruction
  case class SUB() extends Instruction
  case class MUL() extends Instruction
  case class DIV() extends Instruction

  // Logic OPS
  case class CMP() extends Instruction  // [a b <], -1 if a is larger, 0 if equal, 1 if b is larger

  case class JGT(to : LBL) extends Instruction  // greater than
  case class JLT(to : LBL) extends Instruction  // less than
  case class JEQ(to : LBL) extends Instruction  // equal to

  // Jump if top-stack value is FALSE
  case class JF(to : LBL) extends Instruction

  // Jump if top-stack value is TRUE
  case class JT(to : LBL) extends Instruction

  // Jump unconditionally, toSkip must be a positive value with the number of instructions forward to jump
  case class JMP(to : LBL) extends Instruction

  case class LBL(label : Int) extends Instruction

  // Stop execution immediately
  case class HALT() extends Instruction
}

trait Action

trait TargettedAction extends Action

trait Target

trait Cond

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
case class Policy(block:Block, attempt: Option[Attempts] = None) extends AST

case class Block(statements:List[AST]) extends AST

case class ConditionalStatement(cond:AST, pos:AST, neg:AST) extends AST

// List of conditions and their statements
case class ConditionalList(blocks:List[(AST, AST)], neg:AST) extends AST

case class ActionLiteral(name:String) extends AST

// Specific actions
case class Call(target: Target) extends AST
case class Text(target: Target) extends AST
case class Email(target: Target) extends AST

case class Wait(duration: Duration) extends AST

// count must be > 1, period must be >= longest path through the policy
case class Attempts(count:Int, period: Option[Duration]) extends AST

// Generic actions
case class Page(target: Target) extends AST
case class Notify(target: Target) extends AST

case class TargetName(name: String) extends AST

case class User(name: String) extends AST with Target
case class Team(name: String) extends AST with Target
case class Service(name: String) extends AST with Target

// Basic Ops

case class EqOp(left: AST, right:AST) extends AST
case class GtOp(left: AST, right:AST) extends AST
case class GteOp(left: AST, right:AST) extends AST
case class LtOp(left: AST, right:AST) extends AST
case class LteOp(left: AST, right:AST) extends AST

case class AndOp(l:AST, r:AST) extends AST   // a and b
case class OrOp(l:AST, r:AST) extends AST    // a or b
case class NotOp(c:AST) extends AST          // ! a

// MomentVal?
//case class TimeRangeVal(value: String) extends AST
//case class DateRangeVal(value: String) extends AST

case class ImpactVal(value: Int) extends AST
case class TagVal(value: String) extends AST
case class TrueVal() extends AST
case class FalseVal() extends AST
case class NotExpr(cond: Cond) extends AST
case class IntVal(value: Int) extends AST
case class BooleanVal(value: Boolean) extends AST

case class UnitVal() extends AST

// Representing an empty blocks of statements
case class Empty() extends AST

case class Property(context: String, field: String) extends AST

// Flow control:
//  attempt     :: executes the policy up to N times, may only occur once at the start of the policy, N must be > 1, optional duration
//  if          :: executes blocks of actions conditionally
//  wait        :: wait a set amount of time (e.g. 2h)
//  wait_until  :: waits until some time (e.g. 2am)
//  escalate    :: increases the urgency one level and repeats the policy, if already at maximum urgency then just repeats
//  escalate_to :: halts the policy and passes the incident to the named policy
//  halt        :: halts the policy, takes an optional message
//  abandon     :: halts the policy, takes an optional message
//  next        :: ? start execution of the next iteration, if no iterations left then abandon's the incident (should be a path dependent statement)
//  repeat?

// Functions to use as part of conditional flow control:
//  during   :: true if the current time is in the specified time-window (e.g. not 2am to 4am)
//  running  :: returns a duration specifying how long the policy has been executing, may be compared to another duration object (e.g. 4h)
//  runs     :: returns an int true the first N times through the policy, false afterwards
//  regex    :: returns true if the regex matches
//

// Incident lifecycle
//  start in 'new' state
//  if policy exists, move to 'alerting'
//  if policy does not exist, move to 'open'
//  if policy halts early, move to 'open'
//  if policy abandons early or times out, move to 'abandoned'

// At any point a user can intervene and ack an incident, or just immediately archive the incident
// new/open -> ack -> resolve (manually opened incidents only) -> archive
// new/open -> ack -> archive
// archive -> ack

// At any point a rule can fired a Resolved Alert and resolve an incident
// * -> resolve (auto) -> archive

// should be able to handle suppressed notifications in a policy (subscribe rules)

// statements appearing after the repeat are assumed to execute once the alert has been acked

// any policy that ceases execution before the incident is acked, puts the incident into an "abandoned" state
// abandoned incidents should appear at the top of any incident dashboard, along with incidents that have been
// open overly long and are close to becoming abandoned

// incidents can be suppressed

class NotificationPolicyParser extends RegexParsers {

  // statements ::= expr +
  def policy: Parser[AST] = opt(attempt_st) ~ statements ^^ {
    case at ~ body => Policy(body, at)
  }

  def statements: Parser[Block] = rep(statement)^^Block

  def statement: Parser[AST] = action_st | wait_st | cond_1 | cond_2 | cond_n // | next_st | halt_st | escalate | invoke schedule/policy

  def attempt_st: Parser[Attempts] = (("attempt" ~> intLiteral <~ "times") ~ opt("every" ~> duration)) ^^ {
    case t ~ du if t.value > 1 => Attempts(t.value, du)
  }

  def cond_1: Parser[AST] =
    ("if" ~> expr <~ "then") ~ (statements <~ "end")^^{
      case c ~ p => ConditionalStatement(c,p,UnitVal())
  }

  def cond_2: Parser[AST] =
    ("if" ~> expr <~ "then") ~
    statements ~
    ("else" ~> statements <~ "end")^^{
    case c ~ p ~ n => ConditionalStatement(c,p,n)
  }

  def cond_n: Parser[AST] =
    ("if" ~> expr <~ "then") ~ statements ~
    rep(("elif" ~> expr <~ "then") ~ statements) ~
    (("else" ~> statements <~ "end") | "end")^^{
      case c1 ~ b1 ~ blocks ~ (otherwise:AST) =>
        // Destructured into pairs of conditions and their statements
        val l = for (block <- blocks) yield (block._1, block._2)
        ConditionalList(List((c1,b1)) ::: l,otherwise)
      case c1 ~ b1 ~ blocks ~ (end:String) =>
        // Destructured into pairs of conditions and their statements
        val l = for (block <- blocks) yield (block._1, block._2)
        ConditionalList(List((c1,b1)) ::: l,UnitVal())
  }

  def expr: Parser[AST] = compare_op | logic_op | terminal | ( "(" ~> expr <~ ")")

  def terminal: Parser[AST] = impactLiteral | tagLiteral | intLiteral | boolLiteral | property

  def wait_st: Parser[AST] = ("wait" ~> duration)^^{
    case d => Wait(d)
  }

  def logic_op: Parser[AST] = andOp | orOp | notOp

  def condOp: Parser[AST] = chainl1(terminal,
    "<"^^{op => (left:AST, right:AST) => LtOp(left, right)}
  )

  def andOp: Parser[AST] = (expr ~ "&&" ~ expr)^^{case l~_~r => AndOp(l,r)}
  def orOp: Parser[AST] = (expr ~ "||" ~ expr)^^{case l~_~r => OrOp(l,r)}
  def notOp: Parser[AST] = ("!" ~> expr)^^{case c => NotOp(c)}

  def compare_op: Parser[AST] = lt | gt | lte | gte | eq

  def lt: Parser[AST] = (terminal ~ "<" ~ terminal)^^{case l~_~r => LtOp(l,r)}
  def lte: Parser[AST] = (terminal ~ "<=" ~ terminal)^^{case l~_~r => LteOp(l,r)}

  def eq: Parser[AST] = (terminal ~ "==" ~ terminal)^^{case l~_~r => EqOp(l,r)}
  def notEq: Parser[AST] = (terminal ~ "!=" ~ terminal)^^{case l~_~r => NotOp(EqOp(l,r))}

  def gt: Parser[AST] = (terminal ~ ">" ~ terminal)^^{case l~_~r => GtOp(l,r)}
  def gte: Parser[AST] = (terminal ~ ">=" ~ terminal)^^{case l~_~r => GteOp(l,r)}

  def duration: Parser[Duration] = intLiteral ~ ("s" | "m" | "h" | "d") ^^ {
    case v ~ "s" => Duration.standardSeconds(v.value)
    case v ~ "m" => Duration.standardMinutes(v.value)
    case v ~ "h" => Duration.standardHours(v.value)
    case v ~ "d" => Duration.standardDays(v.value)
  }

  def action_st: Parser[AST] = ("call" | "email" | "page" | "notify") ~ target ^^ {
    case "email" ~ t => Email(t)
    case "call" ~ t => Call(t)
    case "text" ~ t => Text(t)
  }

  def target: Parser[Target] = (("user" | "team" | "service") ~ ("("~>targetName<~")"))^^{
    case "user" ~ tn => User(tn.name)
    case "team" ~ tn => Team(tn.name)
    case "service" ~ tn => Service(tn.name)
  }

  def targetName: Parser[TargetName] = (userEmailLiteral | teamOrServiceName) ^?{case n => n}^^TargetName

  def teamOrServiceName : Parser[String] = """[A-Za-z_][a-zA-Z0-9]*""".r
  def userEmailLiteral: Parser[String] = """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r

  def impactLiteral : Parser[ImpactVal] = ("I"~>"("~>"""[1-5]""".r<~")")^^{value => ImpactVal(value.toInt)}

  def tagLiteral : Parser[TagVal] = """tag\([A-Za-z_][a-zA-Z0-9]*\)""".r^^{value => TagVal(value.toString)}

  def intLiteral : Parser[IntVal] = """[1-9][0-9]*|0""".r^^{
    value => IntVal(value.toInt)}

  // global variables / state
  //  any thing in the incident, team, service
  //  the SLA, the priority matrix, service owner
  // Must be one of:
  // user ::= the user the policy belongs to (may be empty)
  // team ::= the team the policy belongs to (may be empty)
  // service ::= the service the policy belongs to (may be empty)
  // context ::= guaranteed to be one of the above where appropriate
  // incident ::= incident to process
  // rule ::= rule that triggered the incident
  // xxx.foo ::= foo is the name of the property to access
  def property : Parser[Property] = ("incident" | "team" | "user" | "service" | "policy") ~ "." ~ """[a-zA-Z0-9_]*""".r^? {
    case c ~ _ ~ n => Property(c, n)
  }

  // policy.iteration :: INT = number of complete cycles through policy, starts at 0, increments after each repeat statement
  // policy.max_iterations :: INT = total number of cycles the policy will perform, equal to 1 plus the value in the repeat statement
  // policy.is_first_iter :: BOOL = equivaltent to "policy.iteration = 0"
  // policy.is_last_iter :: BOOL = equivalent to "policy.iteration + 1 = policy.max_iterations"

  def boolLiteral : Parser[BooleanVal] = ("true" | "false")^^{
    case "true" => BooleanVal(true)
    case "false" => BooleanVal(false)
  }

  def parse(str:String) = parseAll(policy, str)
}
