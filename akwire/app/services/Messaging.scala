package services

import com.rabbitmq.client._
import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import play.api.Configuration

import org.slf4j.{LoggerFactory, Logger}
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty
import javax.annotation.PostConstruct
import javax.inject.Named
import play.api.libs.json._

import models.AgentId

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
      val sessionBroker = context.actorOf(Props[SessionBroker], "SessionBroker")

      try {
        logger.info("Connecting to RabbitMQ")

        sessionBroker ! ('connect, factory.newConnection())

      } catch {
        case e: Exception =>
          logger.error("Error connecting to RabbitMQ: " + e)
          if (sessionBroker != null) {
            sessionBroker ! 'disconnect
          }
          self ! 'reconnect
      }

    case 'reconnect =>
      Thread.sleep(30 * 1000)
      self ! 'connect
    case m => logger.error("Bad message: " + m)
  }
}

// Listens for new Agents to start broadcasting, registers them, establishes a session,
// and creates a persistent heartbeat between the agent and the manager
class SessionBroker extends Actor with DefaultWrites {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SessionBroker])

  var incomingMessages : QueueingConsumer = null;

  var incomingChannel : Channel = null;
  var outgoingChannel : Channel = null;

  var connected = false;

  var registeredAgents : Map[AgentId, ActorRef] = new collection.immutable.HashMap

  val HUB_TO_AGENT_EXCH = "hub.to.agent"
  val AGENT_TO_HUB_EXCH = "agent.to.hub"

  var nextSessionToken = 0

  def receive = {
    case ('connect, connection : Connection) =>
      try {
        logger.info("Connecting to RabbitMQ")

        incomingChannel = connection.createChannel()
        outgoingChannel = connection.createChannel()

        incomingChannel.exchangeDeclare(AGENT_TO_HUB_EXCH, "direct")

        incomingChannel.queueDeclare(AGENT_TO_HUB_EXCH, false, true, true, null)
        incomingChannel.queueBind(AGENT_TO_HUB_EXCH, AGENT_TO_HUB_EXCH, "")

        incomingMessages = new QueueingConsumer(incomingChannel);

        incomingChannel.basicConsume(AGENT_TO_HUB_EXCH, true, incomingMessages);

        connected = true

        // Start consuming messages
        self ! 'consume

      } catch {
        case e: Exception =>
          logger.error("Error connecting to RabbitMQ: " + e)
          self ! 'reconnect
      }

    case ('publish, agentId : AgentId, msg : JsValue) =>
      self ! ('publish, agentId, msg.toString())

    case ('publish, agentId : AgentId, msg : String) =>

      // use the agentId as the routing key
      // Set mandatory true (message must find a home in some queue)

      outgoingChannel.basicPublish(HUB_TO_AGENT_EXCH, agentId.value, true,
        MessageProperties.PERSISTENT_TEXT_PLAIN,
        msg.getBytes);

    case 'consume =>
      try {
        logger.info("Waiting for messages")
        val request = incomingMessages.nextDelivery();
        self ! ('process, Json.parse(request.getBody()))
      } catch {
        case e: Exception =>
          logger.error("Error with RabbitMQ: " + e)
          self ! 'reconnect
      }

    case ('process, msg : JsValue) =>
      logger.info("Received message: " + msg)

      // route to agent
      // is registration request
      //

      val agentId = new AgentId((msg\"agent_id").as[String])

      if (registeredAgents.contains(agentId) && (msg\"command").as[String] != "issue-session-token") {
        // Route the message to the agent handler
        logger.info("Processing message: " + msg)
        registeredAgents(agentId) ! ('process, msg)
      } else if (registeredAgents.contains(agentId) && (msg\"command").as[String] == "issue-session-token") {
        logger.info("What !?!: " + msg)
        self ! ('publish, agentId, Json.toJson( Map.apply("result" -> "registration-denied", "token" -> nextSessionToken.toString)))
      } else {

        logger.info("command: " + (msg\"command").as[String])

        // Is this a registration request?
        if ((msg\"command").as[String].equals("issue-session-token")) {
          logger.info("Received registration request")

          // assume the request is valid

          val newAgentHandler = context.actorOf(Props[AgentHandler], "agent." + agentId.value)
          registeredAgents += (agentId -> newAgentHandler);

          newAgentHandler ! ('processRegistration, msg)

          // send the session token back to the agent

          nextSessionToken += 1

          self ! ('publish, agentId, Json.toJson( Map.apply("result" -> "registration-accepted", "token" -> nextSessionToken.toString)))

          logger.info("Accepted and registered agent:" + agentId)

        } else {
          logger.info("Message received from unregistered agent")
        }

      }

      // Delivery is done, go back to consuming
      self ! 'consume

    case 'disconnect =>
      connected = false
      incomingChannel.close

    case a => logger.error("Bad message: " + a)
  }
}

// This Actor handles all communication with an agent
class AgentHandler extends Actor {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AgentHandler])

  def receive = {
    case ('processRegistration, msg) =>

    case ('receive, msg: JsValue) =>

    case ('send, msg : JsValue) =>

    case 'disconnect =>

    case a => logger.error("Bad message: " + a)
  }
}
