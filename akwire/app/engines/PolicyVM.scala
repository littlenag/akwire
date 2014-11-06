package engines

// https://gist.github.com/kmizu/1364341

import engines.Runtime.ActionResults
import models.Incident
import org.joda.time.Duration

import scala.util.{Failure, Success, Try}
import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.Map

import play.api.Logger

object PolicyVM {

  def eval(policy: String, incident: Incident): Try[ActionResults] = {
    val parser = new NotificationPolicyParser

    Logger.info("Compiling policy: " + policy)

    parser.parse(policy) match {
      case parser.Success(result, _) =>
        val interpreter = new Interpreter

        // And then the Environment will need to point at the next instruction to execute
        val results = interpreter.eval(new Environment(incident), result.asInstanceOf[AST])
        Success(ActionResults(results))
      case er: parser.NoSuccess =>
        Logger.error("Parse error: " + er)
        Failure(new RuntimeException(er.toString))
    }
  }
}

class Environment(val parent:Option[Environment]){
  import Runtime._

  var incident : Option[Incident] = None;

  def this(incident_ : Incident) = {
    this(None)
    incident = Some(incident_)
  }

  def transact[T <: ActionResult](action: () => T) : List[ActionResult] = {
    println("pre")
    val result = action()
    println("post")

    List(result)
  }

  val variables = Map[String, ActionResults]()
  def apply(key: String): ActionResults = {
    variables.get(key).getOrElse {
      parent.map(_.apply(key)).getOrElse {
        throw new Exception("symbol'%s' not found".format(key))
      }
    }
  }
  def set(key: String, value: ActionResults): ActionResults = {
    def iset(optEnv: Option[Environment]): Unit = optEnv match {
      case Some(env) => if(env.variables.contains(key)) env(key) = value else iset(env.parent)
      case None => ()
    }
    iset(Some(this))
    value
  }
  def update(key: String, value: ActionResults): ActionResults = {
    variables(key) = value
    value
  }
}

class Interpreter {
  import Runtime._
  // FIXME this should actually be a Stream
  def eval(env:Environment, ast:AST): List[ActionResult] = {
    def visit(ast:AST): List[ActionResult] = {
      ast match {
        case Policy(statements, repeat) => {
          val local = new Environment(Some(env))
          eval(local, statements).filterNot(_.isInstanceOf[NullResult])
        }
        case Statements(exprs) => {
          exprs.foldLeft(List.empty[ActionResult]){(result : List[ActionResult], x) => (result ::: (eval(env, x))).asInstanceOf[List[ActionResult]]}
        }
        case Email(User(name)) => {
          env.transact { () =>
            EmailResult(s"emailed: $name")
          }
        }
        case Wait(duration) => {
          List(NullResult())
        }
        case _ => List(NullResult())
      }
    }
    visit(ast)
  }
}

object Runtime {
  case class ActionResults(results:List[ActionResult])

  sealed abstract class ActionResult

  case class CallResult(msg: String) extends ActionResult {
    override def toString() = msg
  }
  case class EmailResult(msg: String) extends ActionResult {
    override def toString() = msg
  }
  case class TextResult(msg: String) extends ActionResult {
    override def toString() = msg
  }

  case class NullResult() extends ActionResult {
    override def toString() = "<null>"
  }
}

trait Action {
  def invoke = throw new RuntimeException("must implement")
}

trait Target {
  def invoke = throw new RuntimeException("must implement")
}


sealed trait AST {
  var node_id = 0
}

// By default policies don't repeat
case class Policy(statements:Statements, repeat: Option[Repeat] = None) extends AST

case class Statements(exprs:List[AST]) extends AST

case class FilterExpr(filter:AST, action:AST) extends AST

case class SeverityFilter(pattern: AST) extends AST
case class SeverityLiteral(value: Int) extends AST

case class TagFilter(pattern: AST) extends AST
case class TagLiteral(value: String) extends AST

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

  def statement: Parser[AST] = filtered_expr | action_expr | wait_expr

  def repeat_expr: Parser[Repeat] = (("repeat" ~> intLiteral <~ "times") ~ opt("every" ~> duration)) ^^ {
    case t ~ du => Repeat(t.value, du)
  }

  //expr ::= filter action target | filter { statements }
  def filtered_expr: Parser[AST] = simple_filtered_expr | nested_filtered_expr

  def simple_filtered_expr : Parser[AST] = filter ~ action_expr ^^{
    case f ~ a => FilterExpr(f,a)
  }

  def nested_filtered_expr : Parser[AST] = filter ~ "{"~>statements<~"}"

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
  def filter: Parser[AST] = severityFilter | tagFilter //| duringFilter

  def wait_expr: Parser[AST] = ("wait" ~> duration)^^{
    case d => Wait(d)
  }

  def duration: Parser[Duration] = intLiteral ~ ("s" | "m" | "h" | "d") ^^ {
    case v ~ "s" => Duration.standardSeconds(v.value)
    case v ~ "m" => Duration.standardMinutes(v.value)
    case v ~ "h" => Duration.standardHours(v.value)
    case v ~ "d" => Duration.standardDays(v.value)
  }

  def action_expr: Parser[AST] = actionLiteral ~ target ^^ {
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
  def severityFilter: Parser[SeverityFilter] = "sev" ~ "("~>sevLiteral<~")" ^^SeverityFilter

  //sevLiteral ::= ["1"-"5"]
  def sevLiteral : Parser[SeverityLiteral] = """[1-5]""".r^^{value => SeverityLiteral(value.toInt)}

  def tagFilter: Parser[TagFilter] = "tag" ~ "("~>tagLiteral<~")" ^^TagFilter
  def tagLiteral : Parser[TagLiteral] = """[A-Za-z_][a-zA-Z0-9]*""".r^^{value => TagLiteral(value.toString)}


  def duringFilter: Parser[AST] = "during" ~ "("~>intLiteral<~")" ^^SeverityFilter

  //intLiteral ::= ["1"-"9"] {"0"-"9"}
  def intLiteral : Parser[IntVal] = """[1-9][0-9]*|0""".r^^{
    value => IntVal(value.toInt)}


  def parse(str:String) = parseAll(policy, str)
}
