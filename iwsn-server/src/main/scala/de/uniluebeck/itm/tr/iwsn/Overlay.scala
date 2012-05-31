package de.uniluebeck.itm.tr.iwsn

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import collection.immutable._
import util.Random
import com.weiglewilczek.slf4s.Logging
import java.util.concurrent.{Executors, TimeUnit}
import akka.util.{FiniteDuration, Duration, Timeout}
import collection.mutable.{Map => MutableMap}

// TODO change strings to URNs!!!

object RpcOperation extends Enumeration {

  type RpcOperation = Value

  val
  ARE_NODES_ALIVE,
  ARE_NODES_ALIVE_SM,
  DESTROY_VIRTUAL_LINK,
  DISABLE_NODE,
  DISABLE_PHYSICAL_LINK,
  ENABLE_NODE,
  ENABLE_PHYSICAL_LINK,
  FLASH_IMAGE,
  FLASH_DEFAULT_IMAGE,
  RESET_NODES,
  SEND_DOWNSTREAM,
  SET_CHANNEL_PIPELINE,
  SET_DEFAULT_CHANNEL_PIPELINE,
  SET_VIRTUAL_LINK = Value

}

import RpcOperation._

sealed trait OverlayMessage

case class RpcRequest(to: Set[String], requestId: Int, op: RpcOperation, args: Any*) extends OverlayMessage

case class RpcResponse(result: Map[String, (Boolean, Option[String])], requestId: Int) extends OverlayMessage

case class RpcProgressRequest(requestId: Int, progressRequestId: Int, to: Set[String]) extends OverlayMessage

case class RpcProgressResponse(progress: Map[String, (Int, Option[String])], requestId: Int, progressRequestId: Int)
  extends OverlayMessage

case class DevicesAttachedEvent(urnsAttached: Set[String]) extends OverlayMessage

case class DevicesDetachedEvent(urns: Set[String]) extends OverlayMessage

case class UpstreamMessage(from: String, payload: Array[Byte]) extends OverlayMessage

case class DownstreamMessage(to: Set[String], payload: Array[Byte]) extends OverlayMessage

class PortalActor extends Actor with Logging {

  class RpcResult(val request: RpcRequest, val client: ActorRef) {

    val responsesReceived: MutableMap[String, (Boolean, Option[String])] = MutableMap()

    def receive(response: RpcResponse): RpcResult = {
      response.result.foreach(entry => responsesReceived += entry)
      this
    }

    def isComplete = request.to.forall(requestedFrom => responsesReceived.contains(requestedFrom))
  }

  class RpcProgressResult(val req: RpcProgressRequest, val client: ActorRef) {

    val responsesReceived: MutableMap[String, (Int, Option[String])] = MutableMap()

    def receive(response: RpcProgressResponse): RpcProgressResult = {
      response.progress.foreach(entry => responsesReceived += entry)
      this
    }

    def isComplete = req.to.forall(requestedFrom => responsesReceived.contains(requestedFrom))
  }

  var devices: Map[ActorRef, Set[String]] = Map()

  var pendingResponses: MutableMap[Int, RpcResult] = MutableMap()

  var pendingProgressResponses: MutableMap[Int, RpcProgressResult] = MutableMap()

  private def gateway = sender

  def receive = {

    case e: DevicesAttachedEvent =>
      logger.debug(e.toString)
      devices.get(gateway) match {
        case Some(gatewayUrns) =>
          devices = devices + ((gateway, (gatewayUrns ++ e.urnsAttached)))
        case None =>
          devices = devices + ((gateway, e.urnsAttached))
      }

    case e: DevicesDetachedEvent =>
      logger.debug(e.toString)
      devices.get(gateway).map(gatewayUrns => devices = devices + ((gateway, (gatewayUrns -- e.urns))))

    case req: RpcRequest =>
      logger.debug(req.toString)
      devices.map(entry => {
        val (gateway, gatewayUrns) = entry
        val intersection = (req.to & gatewayUrns)
        if (!intersection.isEmpty) {
          gateway ! new RpcRequest(intersection, req.requestId, req.op, req.args: _*)
          pendingResponses.put(req.requestId, new RpcResult(req, sender))
        }
      })

    case req: RpcProgressRequest =>
      logger.debug(req.toString)
      devices.map(entry => {
        val (gateway, gatewayUrns) = entry
        val intersection = (req.to & gatewayUrns)
        if (!intersection.isEmpty) {
          gateway ! new RpcProgressRequest(req.requestId, req.progressRequestId, intersection)
          pendingProgressResponses.put(req.requestId, new RpcProgressResult(req, sender))
        }
      })

    case resp: RpcProgressResponse => {

      logger.debug(resp.toString)

      pendingProgressResponses.get(resp.requestId).map(pendingProgressResponse => {

        pendingProgressResponse.receive(resp)

        if (pendingProgressResponse.isComplete) {

          val result = Map(pendingProgressResponse.responsesReceived.toSeq: _*)
          val requestId = pendingProgressResponse.req.requestId
          val progressRequestId = resp.progressRequestId

          pendingProgressResponse.client ! new RpcProgressResponse(result, requestId, progressRequestId)

          pendingProgressResponses = pendingProgressResponses - pendingProgressResponse.req.requestId
        }
      })

      // TODO handle timeouts
    }

    case resp: RpcResponse => {

      logger.debug(resp.toString)

      pendingResponses.get(resp.requestId).map(pendingResponse => {

        pendingResponse.receive(resp)

        if (pendingResponse.isComplete) {

          val result = Map(pendingResponse.responsesReceived.toSeq: _*)
          val requestId: Int = pendingResponse.request.requestId

          pendingResponse.client ! new RpcResponse(result, requestId)

          pendingResponses = pendingResponses - pendingResponse.request.requestId
        }
      })

      // TODO handle timeouts
    }
  }
}

class GatewayActor extends Actor with Logging {

  val PORTAL_ACTOR = "akka://portalActorSystem@localhost:1234/user/portalActor"

  val scheduler = Executors.newScheduledThreadPool(5)

  val operationProgresses: MutableMap[Int, MutableMap[String, (Int, Option[String])]] = MutableMap()

  protected def receive = {

    case req: RpcRequest =>
      logger.debug(req.toString)
      req.op match {
        case RpcOperation.ARE_NODES_ALIVE =>
          areNodesAlive(req)
        case RpcOperation.ARE_NODES_ALIVE_SM =>
          areNodesAliveSm(req)
        case RpcOperation.DESTROY_VIRTUAL_LINK =>
          destroyVirtualLink(req)
        case RpcOperation.DISABLE_NODE =>
          disableNode(req)
        case RpcOperation.DISABLE_PHYSICAL_LINK =>
          disablePhysicalLink(req)
        case RpcOperation.ENABLE_NODE =>
          enableNode(req)
        case RpcOperation.ENABLE_PHYSICAL_LINK =>
          enablePhysicalLink(req)
        case RpcOperation.FLASH_DEFAULT_IMAGE =>
          flashDefaultImage(req)
        case RpcOperation.FLASH_IMAGE =>
          flashImage(req)
        case RpcOperation.RESET_NODES =>
          resetNodes(req)
        case RpcOperation.SEND_DOWNSTREAM =>
          sendDownstream(req)
        case RpcOperation.SET_CHANNEL_PIPELINE =>
          setChannelPipeline(req)
        case RpcOperation.SET_DEFAULT_CHANNEL_PIPELINE =>
          setDefaultChannelPipeline(req)
        case RpcOperation.SET_VIRTUAL_LINK =>
          setVirtualLink(req)
      }

    case req: RpcProgressRequest =>
      logger.debug(req.toString)
      val operationProgress = operationProgresses.get(req.progressRequestId)
      operationProgress match {
        case Some(x) =>
          sender ! new RpcProgressResponse(Map(x.toSeq: _*), req.requestId, req.progressRequestId)
        case None =>
          throw new RuntimeException("Unknown requestId")
      }

    case e: DevicesAttachedEvent =>
      logger.debug(e.toString)
      context.actorFor(PORTAL_ACTOR) ! e

    case e: DevicesDetachedEvent =>
      logger.debug(e.toString)
      context.actorFor(PORTAL_ACTOR) ! e

  }

  private def areNodesAlive(request: RpcRequest) {}

  private def areNodesAliveSm(request: RpcRequest) {}

  private def destroyVirtualLink(request: RpcRequest) {}

  private def disableNode(request: RpcRequest) {}

  private def disablePhysicalLink(request: RpcRequest) {}

  private def enableNode(request: RpcRequest) {}

  private def enablePhysicalLink(request: RpcRequest) {}

  private def flashDefaultImage(request: RpcRequest) {}

  private def flashImage(request: RpcRequest) {

    for (i <- 0 until 99) {
      operationProgresses.put(request.requestId, MutableMap())
      context.system.scheduler.scheduleOnce(Duration(i * 100, TimeUnit.MILLISECONDS), new Runnable() {
        def run() {
          request.to.foreach(urn => {
            val operationProgress = operationProgresses.get(request.requestId)
            operationProgress match {
              case Some(x) =>
                x.put(urn, (i, None))
              case None =>
                throw new RuntimeException("Unknown request ID")
            }
          })
        }
      })
    }

    val map: MutableMap[String, (Boolean, Option[String])] = MutableMap()
    request.to.foreach(urn => {
      map += urn ->(true, None)
    })

    val response: RpcResponse = new RpcResponse(Map(map.toSeq: _*), request.requestId)
    context.system.scheduler.scheduleOnce(Duration(100 * 100, TimeUnit.MILLISECONDS), sender, response)
  }

  private def resetNodes(request: RpcRequest) {

    val map: MutableMap[String, (Boolean, Option[String])] = MutableMap()
    request.to.foreach(urn => {
      map += urn ->(true, None)
    })

    val rpcResponse: RpcResponse = new RpcResponse(Map(map.toSeq: _*), request.requestId)
    context.system.scheduler.scheduleOnce(Duration(500, TimeUnit.MILLISECONDS), sender, rpcResponse)

  }

  private def sendDownstream(request: RpcRequest) {}

  private def setChannelPipeline(request: RpcRequest) {}

  private def setDefaultChannelPipeline(request: RpcRequest) {}

  private def setVirtualLink(request: RpcRequest) {}

}

object Portal extends App with Logging {

  override def main(args: Array[String]) {

    de.uniluebeck.itm.tr.util.Logging.setDebugLoggingDefaults()

    val actorSystem = ActorSystem(
      "portalActorSystem",
      ConfigFactory.parseString(
        """
        akka.actor.provider        = "akka.remote.RemoteActorRefProvider"
        akka.remote.transport      = "akka.remote.netty.NettyRemoteTransport"
        akka.remote.netty.hostname = "localhost"
        akka.remote.netty.port     = 1234
        """
      )
    )

    // actorSystem.eventStream.setLogLevel(Logging.DebugLevel)
    val portalActor = actorSystem.actorOf(Props[PortalActor], "portalActor")

    logger.info("Portal actor (" + portalActor + ") started!")

    val logResult: (Either[Throwable, Any]) => (Either[Throwable, Any]) = {
      result => {
        logger.info("Result: " + result.toString)
        result
      }
    }

    implicit val timeout = Timeout(15, TimeUnit.SECONDS)

    for (ln <- io.Source.stdin.getLines()) {

      if (ln.length() > 0) {
        ln.charAt(0) match {

          case 'r' =>

            println("Sending reset request...")

            val resetFuture = portalActor ? new RpcRequest(
              Set("A", "B", "C"),
              new Random().nextInt(),
              RpcOperation.RESET_NODES
            )

            resetFuture.onComplete(logResult)

          case 'f' => {

            println("Sending flash request")

            val halfSecond: FiniteDuration = Duration(500, TimeUnit.MILLISECONDS)
            val to: Set[String] = Set("A", "B", "C")
            val random: Random = new Random()
            val flashRequestId: Int = random.nextInt()

            val progressSchedule = actorSystem.scheduler.schedule(halfSecond, halfSecond, new Runnable() {
              def run() {
                portalActor.ask(new RpcProgressRequest(random.nextInt(), flashRequestId, to)).onComplete(logResult)
              }
            })
            val flashFuture = portalActor ? new RpcRequest(
              to,
              flashRequestId,
              RpcOperation.FLASH_IMAGE,
              Array(1, 2, 3)
            )

            flashFuture.onComplete(logResult.andThen(result => progressSchedule.cancel()))

          }
        }
      }
    }
  }
}

object Gateway1 extends App with Logging {

  override def main(args: Array[String]) {

    de.uniluebeck.itm.tr.util.Logging.setDebugLoggingDefaults()

    val actorSystem = ActorSystem(
      "gatewayActorSystem",
      ConfigFactory.parseString(
        """
        akka.actor.provider        = "akka.remote.RemoteActorRefProvider"
        akka.remote.transport      = "akka.remote.netty.NettyRemoteTransport"
        akka.remote.netty.hostname = "localhost"
        akka.remote.netty.port     = 2345
        """
      )
    )

    // actorSystem.eventStream.setLogLevel(Logging.DebugLevel)
    val gatewayActor = actorSystem.actorOf(Props[GatewayActor], "gatewayActor")

    logger.info("Gateway actor (" + gatewayActor + ") started!")

    gatewayActor ! new DevicesAttachedEvent(Set("A", "B"))

  }
}

object Gateway2 extends App with Logging {

  override def main(args: Array[String]) {

    de.uniluebeck.itm.tr.util.Logging.setDebugLoggingDefaults()

    val actorSystem = ActorSystem(
      "gatewayActorSystem",
      ConfigFactory.parseString(
        """
        akka.actor.provider        = "akka.remote.RemoteActorRefProvider"
        akka.remote.transport      = "akka.remote.netty.NettyRemoteTransport"
        akka.remote.netty.hostname = "localhost"
        akka.remote.netty.port     = 3456
        """
      )
    )

    // actorSystem.eventStream.setLogLevel(Logging.DebugLevel)
    val gatewayActorRef = actorSystem.actorOf(Props[GatewayActor], "gatewayActor")

    logger.info("Gateway actor (" + gatewayActorRef + ") started!")

    gatewayActorRef ! new DevicesAttachedEvent(Set("C"))

  }
}
