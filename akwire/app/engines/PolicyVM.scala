package engines

// https://gist.github.com/kmizu/1364341

import models.Incident

import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.Map

object Main {

  def eval(policy: String, incident: Incident): Unit = {
    val parser = new NotificationPolicyParser

    val ast = parser.parse(policy).get

    val interpreter = new Interpreter

    // And then the Environment will need to point at the next instruction to execute
    val result = interpreter.eval(new Environment(None), ast)
  }
}

class Environment(val parent:Option[Environment]){
  import Runtime._
  val variables = Map[String, Value]()
  def apply(key: String): Value = {
    variables.get(key).getOrElse {
      parent.map(_.apply(key)).getOrElse {
        throw new Exception("symbol'%s' not found".format(key))
      }
    }
  }
  def set(key: String, value: Value): Value = {
    def iset(optEnv: Option[Environment]): Unit = optEnv match {
      case Some(env) => if(env.variables.contains(key)) env(key) = value else iset(env.parent)
      case None => ()
    }
    iset(Some(this))
    value
  }
  def update(key: String, value: Value): Value = {
    variables(key) = value
    value
  }
}

class Interpreter {
  import Runtime._
  def eval(env:Environment, ast:AST): Value = {
    def visit(ast:AST): Value = {
      ast match{
        case Statements(exprs) =>{
          val local = new Environment(Some(env))
          exprs.foldLeft(UnitValue:Value){(result, x) => eval(local, x)}
        }
        case IfExpr(cond, pos, neg) =>{
          visit(cond) match {
            case BooleanValue(true) => visit(pos)
            case BooleanValue(false) => visit(neg)
            case _ => sys.error("Runtime Error!")
          }
        }
        case LessOp(left, right) =>{
          (visit(left), visit(right)) match {
            case (IntValue(lval), IntValue(rval)) => BooleanValue(lval < rval)
            case _ => sys.error("Runtime Error!")
          }
        }
        case AddOp(left, right) =>{
          (visit(left), visit(right)) match{
            case (IntValue(lval), IntValue(rval)) => IntValue(lval + rval)
            case (StringValue(lval), rval) => StringValue(lval + rval)
            case (lval, StringValue(rval)) => StringValue(lval + rval)
            case _ => sys.error("Runtime Error!")
          }
        }
        case SubOp(left, right) =>{
          (visit(left), visit(right)) match{
            case (IntValue(lval), IntValue(rval)) => IntValue(lval - rval)
            case _ => sys.error("Runtime Error!")
          }
        }
        case MulOp(left, right) =>{
          (visit(left), visit(right)) match{
            case (IntValue(lval), IntValue(rval)) => IntValue(lval * rval)
            case _ => sys.error("Runtime Error!")
          }
        }
        case IntVal(value) => IntValue(value)
        case StringVal(value) => StringValue(value)
        case PrintLine(value) => {
          val v = visit(value);
          println(v);
          v
        }
        case Ident(name) => env(name)
        case ValDeclaration(vr, value) => env(vr) = visit(value)
        case Assignment(vr, value) => env.set(vr, visit(value))
        case func@Func(_, _) => FunctionValue(func, Some(env))
        case FuncDef(name, func) => env(name) = FunctionValue(func, Some(env))
        case FuncCall(func, params) =>{
          visit(func) match{
            case FunctionValue(Func(fparams, proc), cenv) => {
              val local = new Environment(cenv)
              (fparams zip params).foreach{ case (fp, ap) =>
                local(fp) = visit(ap)
              }
              eval(local, proc)
            }
            case _ => sys.error("Runtime Error!")
          }
        }
      }
    }
    visit(ast)
  }
}

object Runtime {
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
  case class FunctionValue(value: Func, env: Option[Environment]) extends Value {
    override def toString() = "function"
  }
  case object UnitValue extends Value {
    override def toString() = "unit"
  }
}

sealed trait AST
case class Statements(exprs:List[AST]) extends AST
case class IfExpr(cond:AST, pos:AST, neg:AST) extends AST

case class SeverityFilter(pattern: AST) extends AST
case class SeverityLiteral(value: Int) extends AST

case class TagFilter(pattern: AST) extends AST
case class TagLiteral(value: String) extends AST

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

  def simple_filtered_expr : Parser[AST] = filter ~ action_expr
  def nested_filtered_expr : Parser[AST] = filter ~ "{"~>statements<~"}"

  def filter: Parser[AST] = severityFilter | tagFilter //| duringFilter

  def action: Parser[AST] = severityFilter | tagFilter //| duringFilter

  // e.g. sev(1); sev(1 to 4); sev(not 3); sev(HIGH)
  def severityFilter: Parser[AST] = "sev" ~ "("~>sevLiteral<~")" ^^SeverityFilter

  //sevLiteral ::= ["1"-"5"]
  def sevLiteral : Parser[AST] = """[1-5]""".r^^{value => SeverityLiteral(value.toInt)}


  def tagFilter: Parser[AST] = "tag" ~ "("~>tagLiteral<~")" ^^SeverityFilter
  
  //tagLiteral ::= ["1"-"5"]
  def tagLiteral : Parser[AST] = """[1-5]""".r^^{value => SeverityLiteral(value.toInt)}

  
  def duringFilter: Parser[AST] = "sev" ~ "("~>intLiteral<~")" ^^SeverityFilter

  //expr ::= cond | if | printLine
  def action_expr: Parser[AST] = assignment|condOp|ifExpr|printLine


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
