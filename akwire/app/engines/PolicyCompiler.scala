package engines

import models.notificationvm.{InstructionSet, Handoff, Program}
import models.notificationvm.Handoff._
import models.Policy
import play.api.Logger


// want to compile the script against the incident
// re-writing terms as necessary
// then the script needs to be executable
// so i'm basically creating a virtual machine for these actions to run on
// where each action is effectively atomic

// encode all this handling an actor that takes care of the runtime
// and saving changes to the runtime

// pagerduty treats this as a simple list of steps, where
// each step may have a delay attached
// and then the whole script just has a repeat counter

// need a barrier primitive, where all actions need to complete before moving forward
//  - to implement the primitive will need to be able to trigger a save of the runtime


object PolicyCompiler {
  import InstructionSet._
  import PolicyAST._

  val VAR_CUR_ITER = "cur_iter"
  val VAR_MAX_ITER = "max_iter"

  val parser = PolicyParser

  class LabelMaker(val start:Int = 0) {
    var cur = start

    def next() = {
      cur = cur + 1
      LBL(cur)
    }
  }

  def compile(sourceCode: String): Either[parser.NoSuccess, List[Instruction]] = {
    Logger.info("Compiling policy: " + sourceCode)

    parser.parse(sourceCode) match {
      case parser.Success(result, _) =>
        // And then the Environment will need to point at the next instruction to execute
        implicit val labelMaker = new LabelMaker()
        val ast = result.asInstanceOf[AST]
        Logger.info(s"ast: #$ast")
        Right(compileAST(ast))
      case er: parser.NoSuccess =>
        Logger.error("Parse error: " + er)
        Left(er)
    }
  }

  private def compileAST(ast:AST)(implicit labeler: LabelMaker): List[Instruction] = {
    def visit(ast:AST): List[Instruction] = {
      ast match {
        case ProgramRoot(statements, repeat_expr) => {

          // Extract the number of times to loop
          val max = repeat_expr match {
            case Some(Attempts(count, _)) => count
            case None => 0
          }

          // If we repeat, then include those statements
          val preamble = if (max > 0) {
            List(PUSH(0), STORE(VAR_CUR_ITER), PUSH(max), STORE(VAR_MAX_ITER))
          } else {
            Nil
          }

          val body = compileAST(statements)

          val counter_inc = if (max > 0) {
            List(LOAD(VAR_CUR_ITER), PUSH(1), ADD(), STORE(VAR_CUR_ITER))
          } else {
            Nil
          }

          val (repeat_instr, body_start) = repeat_expr match {
            case Some(Attempts(count, Some(period))) =>
              (Nil, Nil)
            case Some(Attempts(count, None)) =>
              val body_start = labeler.next()
              // If the repeat > count then jump back to the top of the loop, which would be just after the preamble
              (List(LOAD(VAR_CUR_ITER), LOAD(VAR_MAX_ITER), LT(), JT(body_start)), List(body_start))
            case None =>
              (Nil, Nil)
          }

          preamble ::: body_start ::: body ::: counter_inc ::: repeat_instr ::: List(HALT())
        }

        case Block(exprs) => {
          exprs.foldLeft(List.empty[Instruction]){(result : List[Instruction], x) => (result ::: (compileAST(x)))}
        }

        case ConditionalStatement(cond, pos, empty:Empty) => {
          val cond_expr = compileAST(cond)

          val true_branch = compileAST(pos)

          val after_label = labeler.next()

          // The cond branch must result in a boolean value on the top of the stack
          // if the condition is false, jump past the true branch
          cond_expr ::: List(JF(after_label)) :::
          true_branch ::: List(JMP(after_label)) :::
          List(after_label)
        }

        case ConditionalStatement(cond, pos, neg) => {
          val cond_expr = compileAST(cond)

          val true_branch = compileAST(pos)

          val false_branch_label = labeler.next()
          val after_label = labeler.next()

          val false_branch = compileAST(neg)

          // The cond branch must result in a boolean value on the top of the stack
          // if the condition is false, jump past the true branch
          cond_expr ::: List(JF(false_branch_label)) :::
            true_branch ::: List(JMP(after_label), false_branch_label) :::
            false_branch :::
            List(after_label)
        }

        case Wait(duration) => {
          List(WAIT(duration))
        }

        case EqOp(l,r) => {
          val l_branch = compileAST(l)
          val r_branch = compileAST(r)

          l_branch ::: r_branch ::: List(EQ())
        }

        case NotOp(op) => {
          compileAST(op) ::: List(NEG())
        }

        case Property(context, field) =>
          List(LOAD(context + "." + field))

        case ImpactVal(v) =>
          List(PUSH(v))

        case UrgencyVal(v) =>
          List(PUSH(v))

        case Call(target) =>
          List(LD_TARGET(target),DELIVER(CallTarget))

        case Email(target) =>
          List(LD_TARGET(target),DELIVER(EmailTarget))

        case Text(target) =>
          List(LD_TARGET(target),DELIVER(TextTarget))

        case Notify(target) =>
          List(LD_TARGET(target),DELIVER(NotifyTarget))

        case Page(target) =>
          List(LD_TARGET(target),DELIVER(PageTarget))

        case x => throw new RuntimeException("[compiler] implement me: " + x)
      }
    }
    visit(ast)
  }

  // A pseudo-instruction...
  def LD_TARGET(target:Target) : Instruction = target match {
    case ThisUser() => LOAD("target.thisUser")
    case ThisTeam() => LOAD("target.thisTeam")
    case t : Target => PUSH(t)
  }
}



