package engines

// https://gist.github.com/kmizu/1364341

import engines.InstructionSet.Instruction
import models.Incident
import org.joda.time.{DateTime, Duration}

import scala.util.{Failure, Success, Try}
import scala.util.parsing.combinator.RegexParsers

import play.api.Logger

object PolicyVM {

  def compile(policy: String): Try[List[Instruction]] = {
    val parser = new NotificationPolicyParser

    Logger.info("Compiling policy: " + policy)

    parser.parse(policy) match {
      case parser.Success(result, _) =>
        val compiler = new Compiler

        // And then the Environment will need to point at the next instruction to execute
        val results = compiler.compile(result.asInstanceOf[AST])
        Success(results)
      case er: parser.NoSuccess =>
        Logger.error("Parse error: " + er)
        Failure(new RuntimeException(er.toString))
    }
  }

  def run(program : List[Instruction], incident: Incident, clock: Clock = new StandardClock()) = {

    // should probably name these programs
    //Logger.info("Compiling policy: " + policy)

    // And then the Environment will need to point at the next instruction to execute
    val effects:Stream[Interpreter.Effect] = Interpreter.eval(new Environment(incident, clock), program)

    //effects.dropWhile(! _.isInstanceOf[Stop])

    effects
  }


}

trait Clock {
  def now(): DateTime
}

class StandardClock extends Clock {
  def now() = DateTime.now()
}

class Environment(val incident : Incident, val clock : Clock = new StandardClock()) {

  var programCounter = 0

  def resumeAt(when:DateTime) = {
    while (when.isBefore(clock.now)) {
      Thread.sleep(1000L)
    }
  }

}

object Interpreter {
  import InstructionSet._

  sealed abstract class Effect

  // Program Counter Delta (instructions to move forward/backward by)
  sealed case class PCD(val toSkip : Int = 1) extends Effect
  case class NextI() extends Effect

  case class Stop() extends Effect

  def execute(inst:Instruction): Effect = {
    inst match {
      case EMAIL(who) => {
        NextI()
      }
      case WAIT(duration) => {
        NextI()
      }
      case HALT() =>
        Stop()
      case _ =>
        throw new RuntimeException("implement me!")
    }
  }

  // FIXME this should actually be a Stream
  def eval(env:Environment, program:List[Instruction]): Stream[Effect] = {
    def eval_helper = {
      val effect = execute(program(env.programCounter))
      effect match {
        case PCD(i) =>
          env.programCounter += i
          eval(env, program)
        case NextI() =>
          env.programCounter += 1
          eval(env, program)
        case Stop() =>
          Stream.Empty
      }
    }
    eval_helper
  }
}

class Compiler {
  import InstructionSet._
  // FIXME this should actually be a Stream
  def compile(ast:AST): List[Instruction] = {
    def visit(ast:AST): List[Instruction] = {
      ast match {
        case Policy(statements, repeat) => {
          compile(statements) :+ HALT() // :: compile(repeat) :: Halt()
        }
        case Statements(exprs) => {
          exprs.foldLeft(List.empty[Instruction]){(result : List[Instruction], x) => (result ::: (compile(x)))}
        }
        case FilteredStatement(Conditional(True()), statements) => {
          // Always true, no need to jump just return the branch to execute
          compile(statements)
        }
        case FilteredStatement(Conditional(False()), statements) => {
          // Always false, no need to compile the branch
          Nil
        }
        case FilteredStatement(cond, statements) => {
          val true_branch = compile(statements)

          // if the condition is false, jump past the true branch
          JF(cond, true_branch.length) :: true_branch
        }
        case Email(u @ User(name)) => {
          List(EMAIL(u))
        }
        case Wait(duration) => {
          //env.resumeAt(DateTime.now().plus(duration))
          List(WAIT(duration))
        }
        case _ => Nil
      }
    }
    visit(ast)
  }
}

object InstructionSet {

  sealed abstract class Instruction

  case class CALL(target: Target) extends Instruction
  case class EMAIL(target: Target) extends Instruction
  case class TEXT(target: Target) extends Instruction

  case class WAIT(duration: Duration) extends Instruction

  // Jump if cond evaluates to FALSE, toSkip must be a positive value with the number of instructions forward to jump
  case class JF(cond: Cond, toSkip : Int) extends Instruction

  // Jump if cond evaluates to TRUE, toSkip must be a positive value with the number of instructions forward to jump
  case class JT(cond: Cond, toSkip : Int) extends Instruction

  // Jump unconditionally, toSkip must be a positive value with the number of instructions forward to jump
  case class JMP(toSkip : Int) extends Instruction

  // Stop execution immediately
  case class HALT() extends Instruction
}

trait Action {
  def invoke = throw new RuntimeException("must implement")
}

trait Target {
  def invoke = throw new RuntimeException("must implement")
}

trait Cond {
  def eval(env:Environment) : Boolean = throw new RuntimeException("must implement")
}

sealed trait AST {
  var node_id = 0
}

// By default policies don't repeat
case class Policy(statements:Statements, repeat: Option[Repeat] = None) extends AST

case class Statements(exprs:List[AST]) extends AST

case class FilteredStatement(filter:Conditional, actions:AST) extends AST

case class Conditional(actions:AST) extends AST with Cond

case class SeverityFilter(pattern: AST) extends AST with Cond {
  override def eval(env:Environment) = false
}
case class SeverityLiteral(value: Int) extends AST

case class TagFilter(pattern: AST) extends AST with Cond {
  override def eval(env:Environment) = false
}

case class TagLiteral(value: String) extends AST

case class True() extends AST with Cond {
  override def eval(env:Environment) = true
}

case class False() extends AST with Cond {
  override def eval(env:Environment) = false
}

case class NotFilter(cond: Cond) extends AST with Cond {
  override def eval(env:Environment) = !cond.eval(env)
}

case class ActionLiteral(name:String) extends AST

// Specific actions
case class Call(target: Target) extends AST with Action
case class Text(target: Target) extends AST with Action
case class Email(target: Target) extends AST with Action

case class Wait(duration: Duration) extends AST with Action

case class Repeat(count:Int, period: Option[Duration]) extends AST with Action

// Generic actions
case class Page(target: Target) extends AST with Action
case class Notify(target: Target) extends AST with Action

case class TargetType(name: String) extends AST
case class TargetName(name: String) extends AST

case class User(name: String) extends AST with Target
case class Team(name: String) extends AST with Target
case class Service(name: String) extends AST with Target

//case class DuringFilter(pattern: AST) extends AST
//case class DuringLiteral(value: String) extends AST

case class IntVal(value: Int) extends AST
//case class Ident(name: String) extends AST

class NotificationPolicyParser extends RegexParsers {

  // statements ::= expr +
  def policy: Parser[AST] = statements ~ opt(repeat_expr) ^^ {
    case s ~ re => Policy(s, re)
  }

  def statements: Parser[Statements] = rep(statement)^^Statements

  def statement: Parser[AST] = filtered_expr | wait_expr // | next_expr | halt_expr | escalate | invoke schedule/policy

  def repeat_expr: Parser[Repeat] = (("repeat" ~> intLiteral <~ "times") ~ opt("every" ~> duration)) ^^ {
    case t ~ du => Repeat(t.value, du)
  }

  //expr ::= filter action target | filter { statements }
  def filtered_expr: Parser[AST] = simple_filtered_expr | nested_filtered_expr

  def simple_filtered_expr : Parser[AST] = opt(conditional) ~ action_expr ^^{
    case Some(f) ~ a => FilteredStatement(f,a.asInstanceOf[AST])
    case None ~ a => FilteredStatement(Conditional(True()),a.asInstanceOf[AST])
  }

  def nested_filtered_expr : Parser[AST] = conditional ~ ("{"~>statements<~"}")^^{
    case f ~ s => FilteredStatement(f,s)
  }

  // filters: runs a set of actions that _____
  //  once     :: executes only one time
  //  times(N) :: executes at most N times, where N is less than the repeat times
  //  during   :: executes in certain well-defined windows of times (e.g. not 2am to 4am)
  //  after    :: executes only after some amount of time has passed
  //  before   :: executes only until some amount of time has passed
  //
  // global variables / state
  //  any thing in the incident, team, service
  //  the SLA, the priority matrix, service owner
  def conditional: Parser[Conditional] = severityFilter | tagFilter //| duringFilter

  def wait_expr: Parser[AST] = ("wait" ~> duration)^^{
    case d => Wait(d)
  }

  def duration: Parser[Duration] = intLiteral ~ ("s" | "m" | "h" | "d") ^^ {
    case v ~ "s" => Duration.standardSeconds(v.value)
    case v ~ "m" => Duration.standardMinutes(v.value)
    case v ~ "h" => Duration.standardHours(v.value)
    case v ~ "d" => Duration.standardDays(v.value)
  }

  def action_expr: Parser[Action] = actionLiteral ~ target ^^ {
    case ActionLiteral("email") ~ t => Email(t)
    case ActionLiteral("call") ~ t => Call(t)
    case ActionLiteral("text") ~ t => Text(t)
  }

  def actionLiteral: Parser[ActionLiteral] = ("call" | "page" | "email" | "notify")^^ActionLiteral

  def target: Parser[Target] = (targetType ~ ("("~>targetName<~")"))^^{
    case TargetType("user") ~ tn => User(tn.name)
    case TargetType("team") ~ tn => Team(tn.name)
    case TargetType("service") ~ tn => Service(tn.name)
  }

  def targetType: Parser[TargetType] = ("user" | "team" | "service")^^TargetType

  def targetName: Parser[TargetName] = (userEmailLiteral | teamOrServiceName) ^?{case n => n}^^TargetName

  def teamOrServiceName : Parser[String] = """[A-Za-z_][a-zA-Z0-9]*""".r
  def userEmailLiteral: Parser[String] = """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r

  // e.g. sev(1); sev(1 to 4); sev(not 3); sev(HIGH)
  def severityFilter: Parser[Conditional] = "sev" ~ "("~>sevLiteral<~")" ^^{
    case l => Conditional(SeverityFilter(SeverityLiteral(l.value)))
  }

  //sevLiteral ::= ["1"-"5"]
  def sevLiteral : Parser[SeverityLiteral] = """[1-5]""".r^^{value => SeverityLiteral(value.toInt)}

  def tagFilter: Parser[Conditional] = "tag" ~ "("~>tagLiteral<~")" ^^{
    case t => Conditional(TagFilter(TagLiteral(t.value)))
  }

  def tagLiteral : Parser[TagLiteral] = """[A-Za-z_][a-zA-Z0-9]*""".r^^{value => TagLiteral(value.toString)}


  def duringFilter: Parser[AST] = "during" ~ "("~>intLiteral<~")" ^^SeverityFilter

  //intLiteral ::= ["1"-"9"] {"0"-"9"}
  def intLiteral : Parser[IntVal] = """[1-9][0-9]*|0""".r^^{
    value => IntVal(value.toInt)}


  def parse(str:String) = parseAll(policy, str)
}
