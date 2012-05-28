package de.uniluebeck.itm.tr.iwsn

import java.util.concurrent.{TimeUnit, Executors}
import akka.actor._
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import collection.immutable._
import util.Random
import com.weiglewilczek.slf4s.Logging

sealed trait OverlayMessage

abstract case class RpcRequest(to: Set[String], id: Int) extends OverlayMessage

abstract case class RpcStatus(from: Set[String], id: Int) extends OverlayMessage

abstract case class RpcResponse(from: Set[String], id: Int) extends OverlayMessage

case class FlashNodesRequest(override val to: Set[String], override val id: Int, binaryImage: Array[Byte])
  extends RpcRequest(to, id)

case class FlashNodesStatus(override val from: Set[String], override val id: Int, progress: Int, msg: Option[String])
  extends RpcStatus(from, id)

case class FlashNodesResponse(override val from: Set[String], override val id: Int) extends RpcResponse(from, id)

case class DevicesAttachedEvent(urnsAttached: Set[String]) extends OverlayMessage

case class DevicesDetachedEvent(urns: Set[String]) extends OverlayMessage

case class UpstreamMessage(from: String, payload: Array[Byte]) extends OverlayMessage

case class DownstreamMessage(to: Set[String], payload: Array[Byte]) extends OverlayMessage

class PortalActor extends Actor with Logging {

  var devices: Map[ActorRef, Set[String]] = Map()

  var requests: Map[Int, ActorRef] = Map()

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
          gateway ! new FlashNodesRequest(intersection, request.id, request.binaryImage)
        }
      })

    case status: FlashNodesStatus =>
      logger.debug(status.toString)
      requests.get(status.id).map(clientRef => clientRef ! status)

    case response: RpcResponse =>
      logger.debug(response.toString)
      requests.get(response.id).map(clientRef => clientRef ! response)

  }
}

class GatewayActor(val portalActor: ActorRef) extends Actor with Logging {

  protected def receive = {

    case request: FlashNodesRequest =>
      logger.debug(request.toString)
      for (i <- 0 until 99) {
        Thread.sleep(10)
        sender ! new FlashNodesStatus(request.to, request.id, i, None)
      }
      sender ! new FlashNodesResponse(request.to, request.id)

    case event: DevicesAttachedEvent =>
      logger.debug(event.toString)
      portalActor ! event

    case event: DevicesDetachedEvent =>
      logger.debug(event.toString)
      portalActor ! event

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

    Thread.sleep(4000)

    object requester extends Actor with Logging {

      protected def receive = {
        case status: FlashNodesStatus =>
          logger.debug(status.toString)
        case response: FlashNodesResponse =>
          logger.debug(response.toString)
      }

      def request() {
        portalActor ! new FlashNodesRequest(
          Set("A, B, C"),
          new Random().nextInt(),
          Array.ofDim(1)
        )
      }
    }

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate(new Runnable {
      def run() {
        requester.request()
      }
    }, 3, 30, TimeUnit.SECONDS
    )

  }
}

object Gateway extends App with Logging {


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

  }
}