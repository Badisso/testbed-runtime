package de.uniluebeck.itm.tr.iwsn

import com.weiglewilczek.slf4s.Logging
import akka.actor.{Props, ActorSystem}
import java.util.concurrent.TimeUnit
import util.Random
import akka.util.{Duration, FiniteDuration, Timeout}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory

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
              new Random().nextInt(),
              Set("A", "B", "C"),
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
              flashRequestId,
              to,
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