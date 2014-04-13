package services

import com.rabbitmq.client._
import akka.actor.Actor

import org.slf4j.{LoggerFactory, Logger}
import javax.inject.{Named, Singleton}

//@Named("rabbitmq")
//@Singleton
class Rabbitmq extends Actor {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Rabbitmq])

  val factory = new ConnectionFactory()
  factory.setHost("localhost")

  val QUEUE_NAME = "akwire-command"

  var consumer : QueueingConsumer = null;

  var connected = false;

  logger.debug("RabbitMQ Service Started")


  def receive = {
    case 'connect =>
      try {
        logger.debug("Connecting to RabbitMQ")

        val connection = factory.newConnection()
        val channel = connection.createChannel()

        channel.queueDeclare(QUEUE_NAME, false, false, false, null)

        consumer = new QueueingConsumer(channel);
        channel.basicConsume(QUEUE_NAME, true, consumer);

        connected = true

        // Start consuming messages
        self ! 'consume

      } catch {
        case e: Exception =>
          logger.error("Error connecting to RabbitMQ: " + e)
          self ! 'reconnect
      }

    case 'consume =>
      try {
        logger.debug("Waiting for message")
        val delivery = consumer.nextDelivery();
        self ! ('deliver, new String(delivery.getBody()))
      } catch {
        case e: Exception =>
          logger.error("Error with RabbitMQ: " + e)
          self ! 'reconnect
      }

    case ('deliver, msg : String) =>
      logger.debug("!!! Received message: " + msg)

      // Delivery is done, go back to consuming
      self ! 'consume

    case 'reconnect =>
      connected = false
      Thread.sleep(30 * 1000)
      self ! 'connect

    case a => logger.error("Bad message: " + a)
  }

  // Have the object send itself the kick-off message
  self ! 'connect
}

