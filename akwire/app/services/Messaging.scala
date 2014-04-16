package services

import com.rabbitmq.client._
import akka.actor.{ActorSystem, Props, Actor}
import play.api.Configuration

import org.slf4j.{LoggerFactory, Logger}
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty
import javax.annotation.PostConstruct
import javax.inject.Named
import play.api.libs.json.{JsValue, Json}

@Named
// Using Rabbitmq, this service handles all the messaging between the agents, this app, and external consumers/requestors
class Messaging {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Messaging])

  logger.info("RabbitMQ Service Started")

  @Autowired
  @BeanProperty
  var actorSystem : ActorSystem = null;

  @Autowired
  @BeanProperty
  var dao : Dao = null;

  @Autowired
  @BeanProperty
  var configuration : Configuration = null;

  @PostConstruct
  def init = {
    val factory = new ConnectionFactory()

    factory.setHost(configuration.getString("rabbitmq.host").getOrElse("localhost"))
    factory.setPort(configuration.getString("rabbitmq.port").getOrElse("5672").toInt)

    // actors to:
    //   - watch for new agents
    //   - execute checks against agents
    //   - publish recency data for each agent
    //   - pull data and transform it

    val supervisor = actorSystem.actorOf(Props[Supervisor], "Supervisor")

    // Start the supervisor.
    supervisor ! ('start, factory)
  }

}

class Supervisor extends Actor {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Supervisor])

  def receive = {
    case ('start, factory : ConnectionFactory) =>
      val registrationActor = context.actorOf(Props[RegistrationHandler], "RegistrationActor")

      try {
        logger.info("Connecting to RabbitMQ")

        registrationActor ! ('connect, factory.newConnection())

      } catch {
        case e: Exception =>
          logger.error("Error connecting to RabbitMQ: " + e)
          if (registrationActor != null) {
            registrationActor ! 'disconnect
          }
          self ! 'reconnect
      }

    case 'reconnect =>
      Thread.sleep(30 * 1000)
      self ! 'connect
    case m => logger.error("Bad message: " + m)
  }
}

// Listens for new Agents to start broadcasting, registers them, and creates a persistent heartbeat between
// the agent and the manager
class RegistrationHandler extends Actor {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[RegistrationHandler])

  var consumer : QueueingConsumer = null;

  var channel : Channel = null;

  var connected = false;

  val QUEUE_NAME = "agent-registration-requests"

  def receive = {
    case ('connect, connection : Connection) =>
      try {
        logger.info("Connecting to RabbitMQ")

        channel = connection.createChannel()

        val REGISTRATION_EXCHANGE = "registration"

        channel.exchangeDeclare(REGISTRATION_EXCHANGE, "direct")
        val queueName = channel.queueDeclare().getQueue()
        channel.queueBind(queueName, REGISTRATION_EXCHANGE, "")
        //channel.queueDeclare(QUEUE_NAME, true, false, false, null)

        consumer = new QueueingConsumer(channel);

        channel.basicConsume(queueName, true, consumer);

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
        logger.info("Waiting for registration requests")
        val request = consumer.nextDelivery();
        self ! ('deliver, Json.parse(request.getBody()))
      } catch {
        case e: Exception =>
          logger.error("Error with RabbitMQ: " + e)
          self ! 'reconnect
      }

    case ('deliver, msg : JsValue) =>
      logger.info("Received registration request: " + msg)

      logger.info("From: " + msg\"agent_id")

      // Delivery is done, go back to consuming
      self ! 'consume

    case 'disconnect =>
      connected = false
      channel.close

    case a => logger.error("Bad message: " + a)
  }

}

