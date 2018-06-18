package services.notifications

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.{Regions, Region}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

sealed trait ChannelResponse
case object ChannelSuccess extends ChannelResponse
case object ChannelFailue extends ChannelResponse

sealed trait MessageChannel {
  /**
   * Use a Future here in order to avoid blocking.
   *
   * FIXME use a type class here
   * @param body
   * @param subject
   * @return
   */
  def submit(body:String, subject:String="")(implicit ec: ExecutionContext) : Future[ChannelResponse]
}

case class SnsChannel(accessKey : String,
                      secretKey : String,
                      topicId   : String,
                      region    : Region = Region.getRegion(Regions.US_EAST_1)
                      ) extends MessageChannel {

  import com.amazonaws.services.sns.model.PublishRequest
  import com.amazonaws.services.sns.AmazonSNSClient

  val snsClient = new AmazonSNSClient(new AWSCredentials {
    override def getAWSAccessKeyId: String = accessKey
    override def getAWSSecretKey: String = secretKey
  })

  snsClient.setRegion(region)

  def submit(body:String, subject:String="")(implicit ec: ExecutionContext) : Future[ChannelResponse] = Future {
    Logger.info(s"[SNS] Sending notification. Message: >>>\n$body\n<<<")

    try {
      snsClient.publish(new PublishRequest(topicId, body))
      Logger.debug(s"[SNS] Notification sent.")
    } catch {
      case ex: Exception =>
        Logger.error(s"[SNS] Error sending notification", ex)
    }

    ChannelSuccess
  }
}

case class EmailChannel(to:List[String]) extends MessageChannel {

  import MailHelper._

  def submit(body:String, subject:String)(implicit ec: ExecutionContext): Future[ChannelResponse] = Future {
    Logger.info(s"[EMAIL] Sending notification. Message: >>>\n$body\n<<<")

    send a new Mail (
      from = ("akwire@akwire.com", "Akwire"),  // FIXME move to config
      to = this.to,
      subject = subject,
      message = body
    )

    Logger.debug(s"[EMAIL] Notification sent.")

    ChannelSuccess
  }
}

