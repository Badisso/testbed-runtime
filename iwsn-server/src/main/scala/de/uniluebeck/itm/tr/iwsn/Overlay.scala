package de.uniluebeck.itm.tr.iwsn

import java.util.concurrent.{TimeUnit, Executors}
import akka.actor._
import akka.pattern.ask
import akka.dispatch.Await
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import collection.immutable._
import collection.mutable.{Map => MutableMap, HashMap => MutableHashMap}
import util.Random
import com.weiglewilczek.slf4s.Logging

// TODO change strings to URNs!!!

sealed trait OverlayMessage

abstract case class RpcRequest(to: Set[String], requestId: Int) extends OverlayMessage

abstract case class RpcStatus(from: Set[String], requestId: Int) extends OverlayMessage

abstract case class RpcResponse(from: Set[String], requestId: Int) extends OverlayMessage

case class FlashNodesRequest(override val to: Set[String], override val requestId: Int, binaryImage: Array[Byte])
  extends RpcRequest(to, requestId)

case class FlashNodesStatus(override val from: Set[String], override val requestId: Int, progress: Int,
                            msg: Option[String])
  extends RpcStatus(from, requestId)

case class FlashNodesResponse(override val from: Set[String], override val requestId: Int)
  extends RpcResponse(from, requestId)

case class DevicesAttachedEvent(urnsAttached: Set[String]) extends OverlayMessage

case class DevicesDetachedEvent(urns: Set[String]) extends OverlayMessage

case class UpstreamMessage(from: String, payload: Array[Byte]) extends OverlayMessage

case class DownstreamMessage(to: Set[String], payload: Array[Byte]) extends OverlayMessage

class PortalActor extends Actor with Logging {

  class RpcState(val request: RpcRequest, val client:ActorRef) {

    private var responsesReceivedFrom = Set[String]()

    def receive(response: RpcResponse): RpcState = {
      responsesReceivedFrom = responsesReceivedFrom ++ response.from
      this
    }

    def isComplete = request.to.forall(
      requestedFrom => {
        responsesReceivedFrom.contains(requestedFrom)
      }
    )
  }

  var devices: Map[ActorRef, Set[String]] = Map()

  var requests: Map[Int, RpcState] = Map()

  private def gateway = sender

  override def receive = {

    case event: DevicesAttachedEvent =>
      logger.debug(event.toString)
      devices.get(gateway) match {
        case Some(gatewayUrns) =>
          devices = devices + ((gateway, (gatewayUrns ++ event.urnsAttached)))
        case None =>
          devices = devices + ((gateway, event.urnsAttached))
      }

    case event: DevicesDetachedEvent =>
      logger.debug(event.toString)
      devices.get(gateway) match {
        case Some(gatewayUrns) =>
          devices = devices + ((gateway, (gatewayUrns -- event.urns)))
      }

    case request: FlashNodesRequest =>
      logger.debug(request.toString)
      devices.map(entry => {
        val (gateway, gatewayUrns) = entry
        val intersection = (request.to & gatewayUrns)
        if (!intersection.isEmpty) {
          gateway ! new FlashNodesRequest(intersection, request.requestId, request.binaryImage)
        }
      })

    case status: FlashNodesStatus =>
      logger.debug(status.toString)
      requests.get(status.requestId).map(state => state.client ! status)

    case response: FlashNodesResponse =>
      logger.debug(response.toString)
      // TODO kaputt!
      requests.get(response.requestId).map(state => {
        state.receive(response)
        if (state.isComplete) {
          logger.debug("Request " + response.requestId + " is complete!")
          state.client ! new FlashNodesResponse(state.request.to, state.request.requestId)
          requests = requests - state.request.requestId
        }
      })
      // TODO handle timeouts

  }
}

class GatewayActor(val portalActor: ActorRef) extends Actor with Logging {

  protected def receive = {

    case request: FlashNodesRequest =>
      logger.debug(request.toString)
      for (i <- 0 until 99) {
        Thread.sleep(100)
        sender ! new FlashNodesStatus(request.to, request.requestId, i, None)
      }
      sender ! new FlashNodesResponse(request.to, request.requestId)

    case event: DevicesAttachedEvent =>
      logger.debug(event.toString)
      portalActor ! event

    case event: DevicesDetachedEvent =>
      logger.debug(event.toString)
      portalActor ! event

  }

  def test() {
    println("test")
  }
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

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(new Runnable {
      def run() {
        implicit val timeout = Timeout(15, TimeUnit.SECONDS)
        val future = portalActor ? new FlashNodesRequest(
          Set("A", "B", "C"),
          new Random().nextInt(),
          Array.ofDim(1)
        )
        val result = Await.result(future, timeout.duration).asInstanceOf[FlashNodesResponse]
        println(result)
      }
    }, 15, 15, TimeUnit.SECONDS
    )

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
    val portalActor = actorSystem.actorFor("akka://portalActorSystem@localhost:1234/user/portalActor")
    val gatewayActor = actorSystem.actorOf(Props(new GatewayActor(portalActor)), "gatewayActor")

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
    val portalActor = actorSystem.actorFor("akka://portalActorSystem@localhost:1234/user/portalActor")
    val gatewayActorRef = actorSystem.actorOf(Props(new GatewayActor(portalActor)), "gatewayActor")

    logger.info("Gateway actor (" + gatewayActorRef + ") started!")

    gatewayActorRef ! new DevicesAttachedEvent(Set("C"))

  }
}
