package engines

import models.{Urgency, Impact}

import scala.util.parsing.combinator.RegexParsers

import org.joda.time.{Duration}


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

// base the grammar on java: https://github.com/antlr/grammars-v4/blob/master/java/Java.g4

/**
 * Notification Policy Parser
 * eventually will want a JSON parser as well
 */
object PolicyParser extends RegexParsers {

  import engines.PolicyAST._

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
      case c ~ p => ConditionalStatement(c,p,Empty())
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
        ConditionalList(List((c1,b1)) ::: l,Empty())
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

  // note: actions should be the verb form of what the channel is said to carry
  // need a more general grammar
  //  - invoke policy(foo)
  //  - outboard to SNS, send to message queue, direct email
  //  - want a generic notions, entities have channels they can be contacted on, channels can be one-way or two-way
  //    - push msg to channel+target directly
  //    - push msg to entity, for specific channel, if entity doesn't have that channel can specify default (messages translate)
  //    - invoke policy
  def action_st: Parser[AST] = ("call" | "email" | "text" | "page" | "notify") ~ target ^^ {
    // these chain execution to the target's policy
    case "notify" ~ t => Notify(t)
    case "page" ~ t => Page(t)
    // these immediately put a message on the channel for the target (somewhat context dependent)
    case "email" ~ t => Email(t)
    case "text" ~ t => Text(t)
    case "call" ~ t => Call(t)
  }

  def target: Parser[Target] = (("user" | "team" | "service") ~ ("("~>(intLiteral)<~")"))^^{
    case "user" ~ id => User(id.value)
    case "team" ~ id => Team(id.value)
    case "service" ~ id => Service(id.value)
  }

  // probably want off-board to SNS, plain email, SMS
  def emailLiteral: Parser[String] = """\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}\b""".r

  // levels in reverse index order 0 = highest, 9 = lowest
  // users can allocate and name their levels however they would like, UI can take care of making pretty
  def impactLiteral : Parser[ImpactVal] = ("IL-" ~ """[0-9]""".r)^^{
    case value => ImpactVal(Impact.withName(value._1 + value._2))
  }

  def urgencyLiteral : Parser[UrgencyVal] = ("UL-" ~ """[0-9]""".r)^^{
    case value => UrgencyVal(Urgency.withName(value._1 + value._2))
  }

  //def priorityLiteral : Parser[PriorityLevel] = ("PL-"~"""[0-9]""".r)^^{value => PriorityLevel(Priority.withName(value._1 + value._2))}

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
