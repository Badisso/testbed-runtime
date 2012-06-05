package de.uniluebeck.itm.tr.iwsn

import com.weiglewilczek.slf4s.Logging
import com.typesafe.config.ConfigFactory
import akka.actor.{Props, ActorSystem}

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