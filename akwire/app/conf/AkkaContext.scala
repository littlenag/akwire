package conf

import scala.concurrent.ExecutionContext
import akka.util.Timeout
import scala.concurrent.duration._

object AkkaContext {
  // This will provide context for akka messaging
  implicit val ec = ExecutionContext.Implicits.global
  implicit val timeOut = Timeout(3 seconds)
  //implicit val duration = timeOut.duration
}
