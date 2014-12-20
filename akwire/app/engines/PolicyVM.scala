package engines

// https://gist.github.com/kmizu/1364341
// http://www.staff.science.uu.nl/~dijks106/SSM/instructions.html

import engines.InstructionSet.Instruction
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

      case JGT(inst) =>
        val a = stack.pop().asInstanceOf[IntValue].value
        if (a > 0) {
          Some(oldRegisters.copy(pc = inst, ws = None))
        } else {
          NEXT
        }

      case JLT(inst) =>
        val a = stack.pop().asInstanceOf[IntValue].value
        if (a < 0) {
          Some(oldRegisters.copy(pc = inst, ws = None))
        } else {
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

  // FIXME should return either the program or an error
  def compile(policy: String): Either[parser.NoSuccess, Program] = {
    Logger.info("Compiling policy: " + policy)

    parser.parse(policy) match {
      case parser.Success(result, _) =>

        // And then the Environment will need to point at the next instruction to execute
        val instructions = compileAST(result.asInstanceOf[AST])
        Right(new Program(instructions))
      case er: parser.NoSuccess =>
        Logger.error("Parse error: " + er)
        Left(er)
    }
  }


  def compileAST(ast:AST): List[Instruction] = {
    def visit(ast:AST): List[Instruction] = {
      ast match {
        case Policy(statements, repeat_expr) => {

          // Extract the number of times to loop
          val max = repeat_expr match {
            case Some(Repeat(count, _)) => count
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

          val repeat_instr = repeat_expr match {
            case Some(Repeat(count, Some(period))) =>
              Nil
            case Some(Repeat(count, None)) =>
              // If the repeat > count then jump back to the top of the loop, which would be just after the preamble
              List(LD_VAR(VAR_CUR_COUNT), LD_VAR(VAR_MAX_REPEAT), CMP(), JLT(preamble.size))
            case None =>
              Nil
          }

          (preamble ::: body ::: counter_inc ::: repeat_instr) :+ HALT()
        }
        case Statements(exprs) => {
          exprs.foldLeft(List.empty[Instruction]){(result : List[Instruction], x) => (result ::: (compileAST(x)))}
        }
        case ConditionalAction(BooleanVal(true), statements) => {
          // Always true, no need to jump just return the branch to execute
          compileAST(statements)
        }
        case ConditionalAction(BooleanVal(false), statements) => {
          // Always false, no need to compile the branch
          Nil
        }
        case ConditionalAction(cond, statements) => {
          val condition = compileAST(cond)
          val true_branch = compileAST(statements)

          // if the condition is false, jump past the true branch
          condition ::: (JF(true_branch.length) :: true_branch)
        }
        case Email(u @ User(name)) => {
          List(EMAIL(u))
        }
        case Wait(duration) => {
          List(WAIT(duration))
        }
        case _ => Nil
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

  case class JGT(to : Int) extends Instruction  // greater than
  case class JLT(to : Int) extends Instruction  // less than
  case class JEQ(to : Int) extends Instruction  // equal to

  // Jump if top-stack value is FALSE
  case class JF(to : Int) extends Instruction

  // Jump if top-stack value is TRUE
  case class JT(to : Int) extends Instruction

  // Jump unconditionally, toSkip must be a positive value with the number of instructions forward to jump
  case class JMP(toSkip : Int) extends Instruction

  // Stop execution immediately
  case class HALT() extends Instruction
}

trait Target

sealed trait AST

// By default policies don't repeat
case class Policy(statements:Statements, repeat: Option[Repeat] = None) extends AST

case class Statements(exprs:List[AST]) extends AST

case class Cases(cases:List[AST]) extends AST

case class ConditionalAction(expr:AST, actions:AST) extends AST

case class LT(l:AST, r:AST) extends AST    // <
case class LTE(l:AST, r:AST) extends AST   // <=
case class EQ(l:AST, r:AST) extends AST    // =
case class GT(l:AST, r:AST) extends AST    // >
case class GTE(l:AST, r:AST) extends AST   // >=

case class AND(l:AST, r:AST) extends AST   // a and b
case class OR(l:AST, r:AST) extends AST    // a or b
case class NOT(c:AST) extends AST          // ! a

// filters: runs a set of actions that _____
//  once     :: executes only one time
//  times(N) :: executes at most N times, where N is less than the repeat times
//  during   :: executes in certain well-defined windows of times (e.g. not 2am to 4am)
//  after    :: executes only after some amount of time has passed
//  before   :: executes only until some amount of time has passed
//
// global variables / state
//  any thing in the incident, user, team, service
//  the SLA, the priority matrix, service owner

//case class SeverityFilter(pattern: AST) extends AST with Cond
//case class SeverityLiteral(value: Int) extends AST
//case class TagFilter(pattern: AST) extends AST with Cond
//case class TagLiteral(value: String) extends AST
//case class DuringFilter(pattern: AST) extends AST
//case class DuringLiteral(value: String) extends AST

// Specific actions
case class Call(target: Target) extends AST
case class Text(target: Target) extends AST
case class Email(target: Target) extends AST

case class Wait(duration: Duration) extends AST

case class Repeat(count:Int, period: Option[Duration]) extends AST

// Generic actions
case class Page(target: Target) extends AST
case class Notify(target: Target) extends AST

case class TargetName(name: String) extends AST

case class User(name: String) extends AST with Target
case class Team(name: String) extends AST with Target
case class Service(name: String) extends AST with Target

case class IntVal(value: Int) extends AST
case class BooleanVal(value: Boolean) extends AST
case class Property(name: String) extends AST

class NotificationPolicyParser extends RegexParsers {

  // statements ::= expr +
  def policy: Parser[AST] = statements ~ opt(repeat) ^^ {
    case s ~ re => Policy(s, re)
  }

  def statements: Parser[Statements] = rep(statement)^^Statements

  def statement: Parser[AST] = line | block | wait_st // | next_expr | halt_expr | escalate | invoke schedule/policy

  def repeat: Parser[Repeat] = (("repeat" ~> intLiteral <~ "times") ~ opt("every" ~> duration)) ^^ {
    case t ~ du => Repeat(t.value, du)
  }

  def line : Parser[AST] = opt(conditional) ~ action_expr ^^{
    case Some(cond) ~ action => ConditionalAction(cond,action)
    case None ~ action => ConditionalAction(BooleanVal(true),action)
  }

  def block : Parser[AST] = property ~ ("{" ~> cases <~ "}")^^{
    case p ~ c => ConditionalAction(p,c)
  }

  def cases : Parser[AST] = rep(case_line)^^Cases
  
  //def case_line: Parser[AST] = terminal ~ "=>" ~ statement

  def conditional: Parser[AST] = ( "(" ~> conditional <~ ")") | logic_op | compare_op | terminal

  def logic_op: Parser[AST] = and | or | not

  def and: Parser[AST] = (conditional ~ ("and" | "&&") ~ conditional)^^{case l~_~r => AND(l,r)}
  def or: Parser[AST] = (conditional ~ ("or" | "||") ~ conditional)^^{case l~_~r => OR(l,r)}
  def not: Parser[AST] = ("!" ~> conditional)^^{case c => NOT(c)}

  def compare_op: Parser[AST] = lt | gt | lte | gte | eq

  def terminal: Parser[AST] = intLiteral | boolLiteral | property


  def lt: Parser[AST] = (conditional ~ "<" ~ conditional)^^{case l~_~r => LT(l,r)}
  def lte: Parser[AST] = (conditional ~ "<=" ~ conditional)^^{case l~_~r => LTE(l,r)}

  def eq: Parser[AST] = (conditional ~ "=" ~ conditional)^^{case l~_~r => EQ(l,r)}

  def gt: Parser[AST] = (conditional ~ ">" ~ conditional)^^{case l~_~r => GT(l,r)}
  def gte: Parser[AST] = (conditional ~ ">=" ~ conditional)^^{case l~_~r => GTE(l,r)}

  def wait_st: Parser[AST] = ("wait" ~> duration)^^{
    case d => Wait(d)
  }

  def duration: Parser[Duration] = intLiteral ~ ("s" | "m" | "h" | "d") ^^ {
    case v ~ "s" => Duration.standardSeconds(v.value)
    case v ~ "m" => Duration.standardMinutes(v.value)
    case v ~ "h" => Duration.standardHours(v.value)
    case v ~ "d" => Duration.standardDays(v.value)
  }

  def action_expr: Parser[AST] = ("call" | "email" | "page" | "notify") ~ target ^^ {
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

  //sevLiteral ::= ["1"-"5"]
  //def sevLiteral : Parser[SeverityLiteral] = """[1-5]""".r^^{value => SeverityLiteral(value.toInt)}
  //def tagLiteral : Parser[TagLiteral] = """[A-Za-z_][a-zA-Z0-9]*""".r^^{value => TagLiteral(value.toString)}


  //intLiteral ::= ["1"-"9"] {"0"-"9"}
  def intLiteral : Parser[IntVal] = """[1-9][0-9]*|0""".r^^{
    value => IntVal(value.toInt)}

  def boolLiteral : Parser[BooleanVal] = ("true" | "false")^^{
    case "true" => BooleanVal(true)
    case "false" => BooleanVal(false)
  }

  // Must be one of:
  // user ::= the user the policy belongs to (may be empty)
  // team ::= the team the policy belongs to (may be empty)
  // service ::= the service the policy belongs to (may be empty)
  // context ::= guaranteed to be one of the above where appropriate
  // incident ::= incident to process
  // rule ::= rule that triggered the incident
  // xxx.foo ::= foo is the name of the property to access
  def property: Parser[Property] = """incident\.[A-Za-z_][a-zA-Z0-9]*""".r^^Property

  def parse(str:String) = parseAll(policy, str)
}
