package engines

// https://gist.github.com/kmizu/1364341

import engines.Runtime.ActionResults
import models.Incident

import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.Map

object PolicyVM {

  def eval(policy: String, incident: Incident): ActionResults = {
    val parser = new NotificationPolicyParser

    val ast = parser.parse(policy).get

    val interpreter = new Interpreter

    // And then the Environment will need to point at the next instruction to execute
    interpreter.eval(new Environment(incident), ast)
  }
}

class Environment(val parent:Option[Environment]){
  import Runtime._

  var incident : Option[Incident] = None;

  def this(incident_ : Incident) = {
    this(None)
    incident = Some(incident_)
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
  def eval(env:Environment, ast:AST): ActionResults = {
    def visit(ast:AST): ActionResults = {
      ast match {
        case Statements(exprs) =>{
          val local = new Environment(Some(env))
          exprs.foldLeft(ActionResults(Nil)){(result, x) => eval(local, x)}
        }
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

case class Statements(exprs:List[AST]) extends AST
case class IfExpr(cond:AST, pos:AST, neg:AST) extends AST
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

case class LessOp(left: AST, right:AST) extends AST
case class AddOp(left: AST, right:AST) extends AST
case class SubOp(left: AST, right:AST) extends AST
case class MulOp(left: AST, right:AST) extends AST
case class StringVal(value: String) extends AST
case class PrintLine(value: AST) extends AST
case class IntVal(value: Int) extends AST
case class Ident(name: String) extends AST
case class Assignment(variable: String, value: AST) extends AST
case class ValDeclaration(variable: String, value: AST) extends AST

case class Func(params:List[String], proc:AST) extends AST
case class FuncDef(name: String, func: Func) extends AST
case class FuncCall(func:AST, params:List[AST]) extends AST

class NotificationPolicyParser extends RegexParsers {

  // statements ::= expr +
  def statements: Parser[AST] = rep(statement)^^Statements

  def statement: Parser[AST] = filtered_expr | action_expr

  //expr ::= filter action target | filter { statements }
  def filtered_expr: Parser[AST] = simple_filtered_expr | nested_filtered_expr

  def simple_filtered_expr : Parser[AST] = filter ~ action_expr ^^{
    case f ~ a => FilterExpr(f,a)
  }

  def nested_filtered_expr : Parser[AST] = filter ~ "{"~>statements<~"}"

  def filter: Parser[AST] = severityFilter | tagFilter //| duringFilter

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

  def targetName: Parser[TargetName] = """[A-Za-z_][a-zA-Z0-9]*""".r^?{case n => n}^^TargetName

  // e.g. sev(1); sev(1 to 4); sev(not 3); sev(HIGH)
  def severityFilter: Parser[SeverityFilter] = "sev" ~ "("~>sevLiteral<~")" ^^SeverityFilter

  //sevLiteral ::= ["1"-"5"]
  def sevLiteral : Parser[SeverityLiteral] = """[1-5]""".r^^{value => SeverityLiteral(value.toInt)}

  def tagFilter: Parser[TagFilter] = "tag" ~ "("~>tagLiteral<~")" ^^TagFilter
  def tagLiteral : Parser[TagLiteral] = """[A-Za-z_][a-zA-Z0-9]*""".r^^{value => TagLiteral(value.toString)}


  def duringFilter: Parser[AST] = "during" ~ "("~>intLiteral<~")" ^^SeverityFilter

  //expr ::= cond | if | printLine
  def expr: Parser[AST] = assignment|condOp|ifExpr|printLine

  //if ::= "if" "(" expr ")" expr "else" expr
  def ifExpr: Parser[AST] = "if"~"("~>expr~")"~expr~"else"~expr^^{
    case cond~_~pos~_~neg => IfExpr(cond, pos, neg)
  }

  //cond ::= add {"<" add}
  def condOp: Parser[AST] = chainl1(add, "<"^^{op => (left:AST, right:AST) => LessOp(left, right)})

  //add ::= term {"+" term | "-" term}.
  def add: Parser[AST] = chainl1(term, "+"^^{op => (left:AST, right:AST) => AddOp(left, right)}|
      "-"^^{op => (left:AST, right:AST) => SubOp(left, right)})

  //term ::= factor {"*" factor}
  def term : Parser[AST] = chainl1(funcCall, "*"^^{op => (left:AST, right:AST) => MulOp(left, right)})

  def funcCall: Parser[AST] = factor~opt("("~>repsep(expr, ",")<~")")^^{
    case fac~param =>{
      param match{
        case Some(p) => FuncCall(fac, p)
        case None => fac
      }
    }
  }
  //factor ::= intLiteral | stringLiteral | "(" expr ")" | "{" lines "}"
  def factor: Parser[AST] = intLiteral | stringLiteral | ident | anonFun | "("~>expr<~")" | "{"~>statements<~"}"
  //intLiteral ::= ["1"-"9"] {"0"-"9"}
  def intLiteral : Parser[AST] = """[1-9][0-9]*|0""".r^^{
    value => IntVal(value.toInt)}
  //stringLiteral ::= "\"" {"a-zA-Z0-9.."} "\""
  def stringLiteral : Parser[AST] = "\""~> """((?!")(\[rnfb"'\\]|[^\\]))*""".r <~"\"" ^^ StringVal

  def ident :Parser[Ident] = """[A-Za-z_][a-zA-Z0-9]*""".r^?{
    case n if n != "if" && n!= "val" && n!= "println" && n != "def" => n}^^Ident

  def assignment: Parser[Assignment] = (ident <~ "=") ~ expr ^^ {
    case v ~ value => Assignment(v.name, value)
  }

  def val_declaration:Parser[ValDeclaration] = ("val" ~> ident <~ "=") ~ expr ^^ {
    case v ~ value => ValDeclaration(v.name, value)
  }
  // printLine ::= "printLn" "(" expr ")"
  def printLine: Parser[AST] = "println"~"("~>expr<~")"^^PrintLine

  def anonFun:Parser[AST] = (("(" ~> repsep(ident, ",") <~ ")") <~ "=>") ~ expr ^^ {
    case params ~ proc => Func(params.map{_.name}, proc)
  }

  def funcDef:Parser[FuncDef] = "def"~>ident~opt("("~>repsep(ident, ",")<~")")~"="~expr^^{
    case v~params~_~proc => {
      val p = params match{
        case Some(pr) => pr
        case None => Nil
      }
      FuncDef(v.name, Func(p.map{_.name}, proc))
    }
  }

  def parse(str:String) = parseAll(statements, str)
}
