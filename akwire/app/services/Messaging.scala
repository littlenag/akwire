package services

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.rabbitmq.client._
import akka.actor._
import org.bson.types.ObjectId
import play.api.Configuration

import akka.pattern.{ ask, pipe }

import org.slf4j.{LoggerFactory, Logger}
import scaldi.{Injector, Injectable}
import play.api.libs.json._

import com.rabbitmq.client.AMQP.BasicProperties
import org.joda.time.DateTime

import java.util.UUID
import com.rabbitmq.client.QueueingConsumer.Delivery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import play.api.Play.current

import models._
import scala.concurrent.{Promise, Future, Await}
import scala.concurrent.TimeoutException
import scala.util.{Failure, Success}

// Using Rabbitmq, this service handles all the messaging between the agents, this app, and external consumers/requestors
class Messaging(implicit inj: Injector) extends Injectable {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Messaging])

  logger.info("RabbitMQ Service Started")

  val actorSystem = inject[ActorSystem]
  val dao = inject[Dao]
  val configuration  = inject[Configuration]

  var sessionBroker : ActorRef = null

  def init = {
    try {
      val factory = new ConnectionFactory()

      factory.setHost(configuration.getString("rabbitmq.host").getOrElse("localhost"))
      factory.setPort(configuration.getString("rabbitmq.port").getOrElse("5672").toInt)

      // actors to:
      //   - watch for new agents
      //   - execute checks against agents
      //   - publish recency data for each agent
      //   - pull data and transform it

      sessionBroker = actorSystem.actorOf(Props[SessionBroker], "SessionBroker")

      logger.info("Connecting to RabbitMQ")
      sessionBroker ! Connect(factory.newConnection())
    } catch {
      case ex:Exception =>
        logger.error("Unable to init", ex)
    }
  }

  // take arguments at some point, kind of like NRPE
  def invokeCommand(agentId:AgentId, command:String)(implicit timeout : akka.util.Timeout) : Future[CommandResult] = {
    ask(sessionBroker, ExecuteCommand(agentId,command))(timeout).mapTo[Future[CommandResult]].flatMap(x=>x)
  }
}


case class CommandInfo(corrId: UUID, promise: Promise[CommandResult], started: DateTime = new DateTime)

case class ExecuteCommand(agentId: AgentId, command: String) // : Future[JsValue]
case class CommandResult(result:JsValue)

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

  var consumer : QueueingConsumer = null

  var channel : Channel = null

  var connected = false

  // Agents we have observed and registered
  var registeredAgents : Map[AgentId, ActorRef] = new collection.immutable.HashMap

  // Commands currently in-flight
  var commandsInFlight : Map[UUID, CommandInfo] = new collection.immutable.HashMap

  val HUB_TO_AGENT_EXCH = "akwire.hub.to.agent"
  val AGENT_TO_HUB_EXCH = "akwire.agent.to.hub"

  var latched = false

  var staleCommands : Cancellable = null

  import akka.actor.SupervisorStrategy._

  override val supervisorStrategy =
      OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
        case _ =>
          logger.error("major fault")
          Escalate
      }

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

        consumer = new QueueingConsumer(channel)

        channel.basicConsume(AGENT_TO_HUB_EXCH, true, consumer)

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

        val delivery = consumer.nextDelivery(10)
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

      logger.info("Processing message: " + msg)

      // route to agent
      // is registration request

      val agentId = new AgentId((msg\"agent_id").as[String])

      if (registeredAgents.contains(agentId) && (msg\"hello").asOpt[Boolean].isEmpty) {

        val corrId = UUID.fromString(delivery.getProperties.getCorrelationId)

        val cmd = commandsInFlight.get(corrId)

        if (cmd.isDefined) {
          // Route the message to the agent handler
          logger.info("Received response: " + msg)
          cmd.get.promise.success(CommandResult(msg))
          if (!cmd.get.promise.isCompleted) throw new RuntimeException("what!")
          commandsInFlight = commandsInFlight - corrId
          logger.info("Routed response: " + msg)
        } else {
          logger.info("Received response but wasn't promised: " + msg)
        }

      } else if (registeredAgents.contains(agentId) && (msg\"hello").asOpt[Boolean].isDefined) {
        // Ignore these so that we prevent mis-configured agent from DOSing correctly configured ones
        logger.warn("Agent already registered, check config?: " + agentId)
      } else {
        // Agent not registered with manager, process as new agent

        logger.info("Processing un-managed agent")

        // assume the request is valid, would need to check the agent-id etc

        val agentHandler = context.actorOf(Props[AgentHandler], "agent." + agentId.value)
        registeredAgents += (agentId -> agentHandler)

        agentHandler ! Start(agentId)

        logger.info("Accepted and registered agent:" + agentId)
      }

      // Delivery is done, go back to consuming
      self ! Consume

    case ExecuteCommand(agentId, command) =>
      logger.info("Executing Command: " + agentId + " command: " + command)

      val corrId = java.util.UUID.randomUUID()

      val props = new BasicProperties.Builder()
        .correlationId(corrId.toString)
        .replyTo(AGENT_TO_HUB_EXCH)
        .build()

      // use the agentId as the routing key
      // Set mandatory true (message must find a home in some queue)

      val toSend = Json.obj("command" -> command)

      channel.basicPublish(HUB_TO_AGENT_EXCH, agentId.value, true, props, Json.stringify(toSend).getBytes)

      val resultPromise = Promise[CommandResult]
      commandsInFlight += (corrId -> CommandInfo(corrId, resultPromise))

      sender ! (resultPromise.future)

    case CheckForStaleCommands =>
      logger.trace("Checking for stale commands: " + commandsInFlight.size)
      commandsInFlight = commandsInFlight.filter { case (uuid, ci) =>
        if (ci.started.plusSeconds(30).isAfterNow()) {
          logger.trace("Not stale: " + ci)
          true
        } else {
          // Pass a null value indicating failure
          logger.debug("Flushing stale command: " + ci)
          ci.promise.failure(new TimeoutException("Stale command"))
          false
        }
    }

    case ReleaseAgent(agentId) =>
      logger.info("Releasing agent: " + agentId)

      registeredAgents.get(agentId) match {
        case Some(agent) =>
          agent ! AgentStop

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

case class AgentStop()
case class Ping()

// This Actor handles all communication with an agent
class AgentHandler extends Actor {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AgentHandler])

  var agentId: AgentId = null

  var lastHeard: DateTime = null

  var sessionBroker: ActorRef = null

  var heartbeat : Option[Cancellable] = None

  // length of time to wait for Futures
  implicit val timeout = akka.util.Timeout(30 seconds)

  import com.novus.salat.dao.SalatDAO
  import models.mongoContext._

  object AgentsDAO extends SalatDAO[Agent, ObjectId](MongoConnection()("akwire")("agents"))

  def ensureAgent : Agent = {
    // let's do our query
    AgentsDAO.findOne(MongoDBObject("agentId" -> agentId.value)) match {
      case Some(agent:Agent) => agent
      case None =>
        val agent = Agent(ObjectId.get(), agentId.value, "", false, false)
        AgentsDAO.save(agent)
        agent
    }
  }

  def upsertConnected(value : Boolean): Agent = {
    val agent = ensureAgent.copy(connected = value)
    AgentsDAO.save(agent)
    agent
  }

  // needs a become statement

  def receive = {
    case Start(agentId) =>
      logger.info("Starting agent manager:" + agentId)
      this.sessionBroker = sender
      this.agentId = agentId

      upsertConnected(false)

      val result: Future[Future[CommandResult]] = ask(sessionBroker, ExecuteCommand(agentId, "hello-agent")).mapTo[Future[CommandResult]]

      result.flatMap(x=>x).onComplete {
        case Success(cr) =>
          logger.error("Processing response:" + cr)
          // Needs to respond with 'hello-manager'
          val msg = cr.result
          (msg\"response").asOpt[String] match {
            case Some("hello-manager") =>
              logger.info("Managing agent:" + agentId)
              // Ensure that we ping the agent's in order to ensure they are alive
              this.lastHeard = new DateTime()
              heartbeat = Some(context.system.scheduler.schedule(5 seconds, 5 seconds, self, Ping))
              upsertConnected(true)
            case _ =>
              logger.error("Malformed response to command 'hello-agent':" + agentId)
              sessionBroker ! ReleaseAgent(agentId)
          }
        case Failure(t) =>
          // Release the agent upon error
          logger.info("Timeout on 'hello-agent' cmd:" + agentId)
          sessionBroker ! ReleaseAgent(agentId)
      }

    case AgentStop =>
      logger.info("Manager stopping for agent:" + agentId)

      heartbeat match {
        case Some(timer) if !timer.isCancelled => timer.cancel()
        case _ =>
      }

      upsertConnected(false)

    case Ping =>
      if (lastHeard.plusSeconds(10).isBeforeNow) {

        // missed too many pings, kill the handler and release the agent
        logger.warn("Agent not responding, considered failed: " + agentId)
        heartbeat match {
          case Some(timer) if !timer.isCancelled => timer.cancel()
        }
        sessionBroker ! ReleaseAgent(agentId)

      } else {
        logger.debug("Sending heartbeat:" + agentId)

        val result: Future[Future[CommandResult]] = ask(sessionBroker, ExecuteCommand(agentId, "ping")).mapTo[Future[CommandResult]]

        result.flatMap(x=>x).onComplete {
          case Success(cr) =>
            val msg = cr.result
            // Needs to respond with 'pong'
            (msg\"response").asOpt[String] match {
              case Some("pong") =>
                logger.debug("Heartbeat received:" + agentId)
                lastHeard = new DateTime()
              case _ =>
                logger.error("Malformed response to command 'ping':" + agentId)
                sessionBroker ! ReleaseAgent(agentId)
            }
          case Failure(t) =>
            // Release the agent upon error
            logger.info("Timeout on command 'ping':" + agentId)
            sessionBroker ! ReleaseAgent(agentId)
        }
      }

    case a => logger.error("Bad message: " + a)
  }
}
