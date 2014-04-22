package services

import _root_.util.SpringExtentionImpl
import com.rabbitmq.client._
import akka.actor._
import play.api.Configuration

import org.slf4j.{LoggerFactory, Logger}
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty
import javax.annotation.PostConstruct
import javax.inject.Named
import play.api.libs.json._

import com.rabbitmq.client.AMQP.BasicProperties
import org.joda.time.DateTime

import java.util.UUID
import com.rabbitmq.client.QueueingConsumer.Delivery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import reactivemongo.api.{Cursor, Collection, DB}
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

import play.api.Play.current

import models._
import scala.concurrent.Await
import reactivemongo.bson.BSONObjectID

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
    supervisor ! StartWithFactory(factory)
  }
}

case class StartWithFactory(factory:ConnectionFactory)
case class Restart(factory:ConnectionFactory)

class Supervisor extends Actor {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Supervisor])

  def receive = {
    case StartWithFactory(factory) =>
      logger.info("Connecting to RabbitMQ")
      val sessionBroker = context.actorOf(Props[SessionBroker], "SessionBroker")
      sessionBroker ! Connect(factory.newConnection())

    case Restart(factory) =>
      Thread.sleep(30 * 1000)
      self ! StartWithFactory(factory)

    case m => logger.error("Bad message: " + m)
  }
}

case class CommandInfo(corrId: UUID, handler: (JsValue => Unit), started: DateTime = new DateTime)

case class Execute(agentId: AgentId, command: JsValue, handler: (JsValue => Unit))
case class Connect(connection: Connection)
case class Consume()
case class Process(delivery:Delivery)
case class CheckForStaleCommands()
case class Disconnect()

// agentHandler messages
case class Start(agentId: AgentId)
case class ReleaseAgent(agentId: AgentId)

// Listens for new Agents to start broadcasting, registers them, establishes a session,
// and creates a persistent heartbeat between the agent and the manager
class SessionBroker extends Actor with DefaultWrites {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SessionBroker])

  var consumer : QueueingConsumer = null;

  var channel : Channel = null;

  var connected = false;

  // Agents we have observed and registered
  var registeredAgents : Map[AgentId, ActorRef] = new collection.immutable.HashMap

  // Commands currently in-flight
  var commandsInFlight : Map[UUID, CommandInfo] = new collection.immutable.HashMap

  val HUB_TO_AGENT_EXCH = "akwire.hub.to.agent"
  val AGENT_TO_HUB_EXCH = "akwire.agent.to.hub"

  var latched = false

  var staleCommands : Cancellable = null;

  def receive = {
    case Connect(connection) =>
      try {
        logger.info("SessionBroker starting...")

        // Watch for commands that have gone stale
        staleCommands = context.system.scheduler.schedule(1 seconds, 1 seconds, self, CheckForStaleCommands)

        channel = connection.createChannel()

        channel.exchangeDeclare(AGENT_TO_HUB_EXCH, "direct")

        channel.queueDeclare(AGENT_TO_HUB_EXCH, false, true, true, null)
        channel.queueBind(AGENT_TO_HUB_EXCH, AGENT_TO_HUB_EXCH, "")

        consumer = new QueueingConsumer(channel);

        channel.basicConsume(AGENT_TO_HUB_EXCH, true, consumer);

        connected = true

        logger.info("SessionBroker started")

        // Start consuming messages
        self ! Consume

      } catch {
        case e: Exception =>
          logger.error("Error connecting to RabbitMQ: " + e)
          self ! Disconnect
      }

    case Consume =>
      try {
        if (!latched) { logger.info("Waiting for messages") }

        val delivery = consumer.nextDelivery(10);
        if (delivery != null) {
          latched = false
          self ! Process(delivery)
        } else {
          latched = true
          self ! Consume
        }
      } catch {
        case e: Exception =>
          logger.error("Error with RabbitMQ: " + e)
          self ! Disconnect
      }

    case Process(delivery : Delivery) =>
      val msg = Json.parse(delivery.getBody)

      logger.info("Received message: " + msg)

      // route to agent
      // is registration request

      val agentId = new AgentId((msg\"agent_id").as[String])

      if (registeredAgents.contains(agentId) && (msg\"hello").asOpt[Boolean].isEmpty) {

        val corrId = UUID.fromString(delivery.getProperties.getCorrelationId)

        val c = commandsInFlight.get(corrId)

        if (c.isDefined) {
          // Route the message to the agent handler
          logger.info("Received response: " + msg)
          c.get.handler(msg)
        } else {
          logger.info("Received response but initial command not found: " + msg)
        }

      } else if (registeredAgents.contains(agentId) && (msg\"hello").asOpt[Boolean].isDefined) {
        logger.warn("Agent out of sync with manager, already registered: " + agentId)

      } else {
        // Agent not registered with manager, process as new agent

        logger.info("Processing un-managed agent")

        // assume the request is valid, would need to check the agent-id etc

        val agentHandler = context.actorOf(Props[AgentHandler], "agent." + agentId.value)
        registeredAgents += (agentId -> agentHandler);

        agentHandler ! Start(agentId)

        logger.info("Accepted and registered agent:" + agentId)
      }

      // Delivery is done, go back to consuming
      self ! Consume

    case Execute(agentId, command, handler) =>
      logger.info("Executing Command: " + agentId + " command: " + command)

      val corrId = java.util.UUID.randomUUID();

      val props = new BasicProperties.Builder()
        .correlationId(corrId.toString)
        .replyTo(AGENT_TO_HUB_EXCH)
        .build();

      // use the agentId as the routing key
      // Set mandatory true (message must find a home in some queue)
      channel.basicPublish(HUB_TO_AGENT_EXCH, agentId.value, true, props, Json.stringify(command).getBytes);

      commandsInFlight += (corrId -> CommandInfo(corrId, handler))

    case CheckForStaleCommands =>
      commandsInFlight = commandsInFlight.filter { case (uuid, ci) =>
        if (ci.started.plusSeconds(30).isBeforeNow()) {
          // Pass a null value indicating failure
          ci.handler(null)
          true
        } else {
          false
        }
    }

    case ReleaseAgent(agentId) =>
      logger.info("Releasing agent: " + agentId)

      registeredAgents.get(agentId) match {
        case Some(agent) =>
          agent ! Stop

          context.stop(registeredAgents(agentId))
          registeredAgents = registeredAgents - agentId

          logger.info("Released agent: " + agentId)
        case None =>
          logger.info("No agent to release: " + agentId)
      }

    case Disconnect =>
      logger.info("Disconnecting from RabbitMQ")
      connected = false
      staleCommands.cancel()
      channel.close
      context.stop(self)

    case a => logger.error("Bad message: " + a)
  }
}

case class Stop()
case class Ping()

// This Actor handles all communication with an agent
class AgentHandler extends Actor {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AgentHandler])

  var agentId: AgentId = null

  var lastHeard: DateTime = null

  var sessionBroker: ActorRef = null

  var heartbeat : Cancellable = null

  /** The agents collection */
  private def col = ReactiveMongoPlugin.db.collection[JSONCollection]("agents")

  def get : Agent = {
    // let's do our query
    val result = Await.result(col.find(Json.obj("agentId" -> agentId.value)).one[Agent], 5 seconds)

    if (result.isDefined) {
      return result.get
    } else {
      val agent = Agent(BSONObjectID.generate, agentId.value, "", true, true);
      col.save(agent).map {
        case ok if ok.ok =>
        case error => throw new RuntimeException(error.message)
      }
      return agent
    }
  }

  def markAgentConnected(value : Boolean = true) = {
    col.save(get.copy(connected = value))
  }

  def receive = {
    case Start(agentId) =>
      logger.info("Starting agent manager:" + agentId)
      this.sessionBroker = sender
      this.agentId = agentId;

      get
      markAgentConnected(false)

      sessionBroker ! Execute(agentId, Json.obj("command" -> "hello-agent"), (msg:JsValue) => {
        if (msg == null) {
          // Release the agent upon error
          logger.info("Timeout on 'hello-agent' cmd:" + agentId)
          sessionBroker ! ReleaseAgent(agentId)
        } else {
          // Needs to respond with 'hello-manager'
          (msg\"response").asOpt[String] match {
            case Some("hello-manager") =>
              logger.info("Managing agent:" + agentId)
              // Ensure that we ping the agent's in order to ensure they are alive
              this.lastHeard = new DateTime()
              heartbeat = context.system.scheduler.schedule(5 seconds, 5 seconds, self, Ping)
              markAgentConnected()
            case _ =>
              logger.error("Malformed response to command 'hello-agent':" + agentId)
              sessionBroker ! ReleaseAgent(agentId)
          }
        }
      })

    case Stop =>
      logger.info("Manager stopping for agent:" + agentId)

      if (!heartbeat.isCancelled) {
        heartbeat.cancel()
      }

      markAgentConnected(false)

    case Ping =>
      if (lastHeard.plusSeconds(10).isBeforeNow) {

        // missed too many pings, kill the handler and release the agent
        logger.warn("Agent not responding, considered failed: " + agentId)
        heartbeat.cancel()
        sessionBroker ! ReleaseAgent(agentId)

      } else {
        logger.debug("Sending heartbeat:" + agentId)

        sessionBroker ! Execute(agentId, Json.obj("command" -> "ping"), (msg:JsValue) => {
          if (msg == null) {
            // Release the agent upon error
            logger.info("Timeout on command 'ping':" + agentId)
            sessionBroker ! ReleaseAgent(agentId)
          } else {
            // Needs to respond with 'pong'
            (msg\"response").asOpt[String] match {
              case Some("pong") =>
                logger.debug("Heartbeat received:" + agentId)
                lastHeard = new DateTime()
              case _ =>
                logger.error("Malformed response to command 'ping':" + agentId)
                sessionBroker ! ReleaseAgent(agentId)
            }
          }
        })
      }

    case a => logger.error("Bad message: " + a)
  }
}
