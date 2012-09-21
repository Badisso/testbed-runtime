package de.uniluebeck.itm.tr.iwsn

import com.weiglewilczek.slf4s.Logging
import akka.actor.Actor
import collection.mutable.{Map => MutableMap}
import akka.util.Duration
import java.util.concurrent.{TimeUnit, Executors}

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
          sender ! new RpcProgressResponse(req.requestId, req.progressRequestId, Map(x.toSeq: _*))
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

    val response: RpcResponse = new RpcResponse(request.requestId, Map(map.toSeq: _*))
    context.system.scheduler.scheduleOnce(Duration(100 * 100, TimeUnit.MILLISECONDS), sender, response)
  }

  private def resetNodes(request: RpcRequest) {

    val map: MutableMap[String, (Boolean, Option[String])] = MutableMap()
    request.to.foreach(urn => {
      map += urn ->(true, None)
    })

    val rpcResponse: RpcResponse = new RpcResponse(request.requestId, Map(map.toSeq: _*))
    context.system.scheduler.scheduleOnce(Duration(500, TimeUnit.MILLISECONDS), sender, rpcResponse)

  }

  private def sendDownstream(request: RpcRequest) {}

  private def setChannelPipeline(request: RpcRequest) {}

  private def setDefaultChannelPipeline(request: RpcRequest) {}

  private def setVirtualLink(request: RpcRequest) {}

}
