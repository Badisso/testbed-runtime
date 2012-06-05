package de.uniluebeck.itm.tr.iwsn

import com.weiglewilczek.slf4s.Logging
import akka.actor.{ActorRef, Actor}
import collection.mutable.{Map => MutableMap}

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
          gateway ! new RpcRequest(req.requestId, intersection, req.op, req.args: _*)
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

          pendingProgressResponse.client ! new RpcProgressResponse(requestId, progressRequestId, result)

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

          pendingResponse.client ! new RpcResponse(requestId, result)

          pendingResponses = pendingResponses - pendingResponse.request.requestId
        }
      })

      // TODO handle timeouts
    }
  }
}