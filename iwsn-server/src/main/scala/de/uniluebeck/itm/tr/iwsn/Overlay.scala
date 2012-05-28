package de.uniluebeck.itm.tr.iwsn

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import collection.mutable.HashMap
import com.typesafe.config.ConfigFactory
import java.util.concurrent.{TimeUnit, Executors}
import util.Random

sealed trait OverlayMessage

case class RpcRequest(id: Int, payload: Array[Byte]) extends OverlayMessage

case class RpcStatus(id: Int, payload: Array[Byte]) extends OverlayMessage

case class RpcResponse(id: Int, payload: Array[Byte]) extends OverlayMessage

case class DeviceAttachedEvent(urn: String) extends OverlayMessage

case class DeviceDetachedEvent(urn: String) extends OverlayMessage

case class BinaryMessage(payload: Array[Byte]) extends OverlayMessage

class PortalActor extends Actor {

  val devices = new HashMap[String, ActorRef]

  override def receive = {
    case e: DeviceAttachedEvent =>
      println("put(" + e.urn + ", " + sender + ")")
      devices.put(e.urn, sender)
    case e: DeviceDetachedEvent =>
      val removed = devices.remove(e.urn)
      println("remove(" + e.urn + "):" + removed)
  }
}

class GatewayActor extends Actor {

  val portal = context.actorFor("akka://portal@opium.local:8888/portalActor")

  protected def receive = {
    case e: DeviceAttachedEvent =>
      portal ! e
    case e: DeviceDetachedEvent =>
      portal ! e
  }
}

object Portal extends App {

  override def main(args: Array[String]) {
    val config = ConfigFactory.load()
    val actorSystem = ActorSystem("portal", config.getConfig("portal"))
    val portalActor = actorSystem.actorOf(Props[PortalActor], "portalActor")
  }
}

object Gateway extends App {

  override def main(args: Array[String]) {
    val config = ConfigFactory.load()
    val actorSystem = ActorSystem("gateway1", config.getConfig("gateway1"))
    val gatewayActor = actorSystem.actorOf(Props[GatewayActor], "portal")

    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.schedule(new Runnable {
      val random = new Random()
      def run() {
        if (random.nextInt(2) == 0) {
          gatewayActor ! new DeviceAttachedEvent("urn:local:0x1234")
        } else {
          gatewayActor ! new DeviceDetachedEvent("urn:local:0x1234")
        }
      }
    }, 2, TimeUnit.SECONDS
    )

  }
}