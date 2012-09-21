package de.uniluebeck.itm.tr.iwsn

import com.weiglewilczek.slf4s.Logging
import akka.actor.{ActorRef, Actor}
import collection.mutable.{Map => MutableMap}
import java.util.concurrent.TimeUnit
import akka.util.Duration

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

  // TODO per-client state
  val pendingResponses: MutableMap[Int, RpcResult] = MutableMap()

  // TODO per-client state
  val pendingProgressResponses: MutableMap[Int, RpcProgressResult] = MutableMap()

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
      devices.map(
        entry => {

          val (gateway, gatewayUrns) = entry
          val intersection = (req.to & gatewayUrns)

          if (!intersection.isEmpty) {

            gateway ! new RpcRequest(req.requestId, intersection, req.op, req.ttl, req.ttlUnit, req.args: _*)
            pendingResponses.put(req.requestId, new RpcResult(req, sender))

            // schedule timeout
            val duration = Duration(req.ttl, req.ttlUnit)
            val timeoutRunnable = new RpcRequestTimeoutRunnable(req)
            context.system.scheduler.scheduleOnce(duration, timeoutRunnable)
          }
        }
      )

    case req: RpcProgressRequest =>
      logger.debug(req.toString)
      devices.map(
        entry => {

          val (gateway, gatewayUrns) = entry
          val intersection = (req.to & gatewayUrns)

          if (!intersection.isEmpty) {

            gateway ! new RpcProgressRequest(req.requestId, req.progressRequestId, intersection)
            pendingProgressResponses.put(req.requestId, new RpcProgressResult(req, sender))

            // schedule timeout
            val duration = Duration(10, TimeUnit.SECONDS)
            val timeoutRunnable = new RpcProgressRequestTimeoutRunnable(req)
            context.system.scheduler.scheduleOnce(duration, timeoutRunnable)
          }
        }
      )

    case resp: RpcProgressResponse => {

      logger.debug(resp.toString)

      pendingProgressResponses.get(resp.requestId).map(pendingProgressResponse => {

        pendingProgressResponse.receive(resp)

        if (pendingProgressResponse.isComplete) {

          val result = Map(pendingProgressResponse.responsesReceived.toSeq: _*)
          val requestId = pendingProgressResponse.req.requestId
          val progressRequestId = resp.progressRequestId

          try {
            pendingProgressResponse.client ! new RpcProgressResponse(requestId, progressRequestId, result)
          }
          catch {
            case e: Exception =>
              logger.warn("Exception caught while sending progress response to client: " + e.toString)
          }
          finally {
            pendingProgressResponses.remove(pendingProgressResponse.req.requestId)
          }

        }
      })
    }

    case resp: RpcResponse => {

      logger.debug(resp.toString)

      pendingResponses.get(resp.requestId).map(pendingResponse => {

        pendingResponse.receive(resp)

        if (pendingResponse.isComplete) {

          val result = Map(pendingResponse.responsesReceived.toSeq: _*)
          val requestId: Int = pendingResponse.request.requestId

          try {
            pendingResponse.client ! new RpcResponse(requestId, result)
          }
          catch {
            case e: Exception =>
              logger.warn("Exception caught while sending response to client: " + e.toString)
          }
          finally {
            pendingResponses.remove(pendingResponse.request.requestId)
          }
        }
      })
    }
  }

  private class RpcRequestTimeoutRunnable(val req: RpcRequest) extends Runnable {

    def run() {
      PortalActor.this.timeout(req)
    }
  }

  private class RpcProgressRequestTimeoutRunnable(val req: RpcProgressRequest) extends Runnable {

    def run() {
      PortalActor.this.timeout(req)
    }
  }

  private def timeout(req: RpcRequest) {

    // TODO remove schedule when answer arrives early

    val rpcResult = pendingResponses.remove(req.requestId)

    if (rpcResult.isDefined) {

      logger.debug("timeout: " + req.toString)

      val responseMap: MutableMap[String, (Boolean, Option[String])] = MutableMap()

      responseMap ++ rpcResult.get.responsesReceived
      req.to.foreach(urn => {
        if (!responseMap.contains(urn)) {
          responseMap.put(urn, (false, Option("Timeout after " + req.ttl + " " + req.ttlUnit)))
        }
      })

      rpcResult.get.client ! new RpcResponse(req.requestId, Map(responseMap.toSeq: _*))
    }
  }

  private def timeout(req: RpcProgressRequest) {

    // TODO remove schedule when answer arrives early

    val rpcProgressResult = pendingProgressResponses.remove(req.requestId)

    if (rpcProgressResult.isDefined) {

      logger.debug("timeout: " + req.toString)

      val requestId = req.requestId
      val progressRequestId = req.progressRequestId
      val progressMap: MutableMap[String, (Int, Option[String])] = MutableMap()

      progressMap ++ rpcProgressResult.get.responsesReceived
      req.to.foreach(urn => {
        if (!progressMap.contains(urn)) {
          progressMap.put(urn, (-1, Option("Timeout")))
        }
      })

      rpcProgressResult.get.client ! new RpcProgressResponse(requestId, progressRequestId, Map(progressMap.toSeq: _*))
    }
  }
}