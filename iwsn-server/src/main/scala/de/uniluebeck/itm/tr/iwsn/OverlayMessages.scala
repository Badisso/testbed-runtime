package de.uniluebeck.itm.tr.iwsn

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
import java.util.concurrent.TimeUnit

sealed trait OverlayMessage

case class RpcRequest(requestId: Int, to: Set[String], op: RpcOperation, ttl: Int, ttlUnit: TimeUnit, args: Any*)
  extends OverlayMessage

case class RpcResponse(requestId: Int, result: Map[String, (Boolean, Option[String])]) extends OverlayMessage

case class RpcProgressRequest(requestId: Int, progressRequestId: Int, to: Set[String]) extends OverlayMessage

case class RpcProgressResponse(requestId: Int, progressRequestId: Int, progress: Map[String, (Int, Option[String])])
  extends OverlayMessage

case class DevicesAttachedEvent(urnsAttached: Set[String]) extends OverlayMessage

case class DevicesDetachedEvent(urns: Set[String]) extends OverlayMessage

case class UpstreamMessage(from: String, payload: Array[Byte]) extends OverlayMessage

case class DownstreamMessage(to: Set[String], payload: Array[Byte]) extends OverlayMessage