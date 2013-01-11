// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public interface RequestOrBuilder
    extends com.google.protobuf.MessageOrBuilder {
  
  // required int64 requestId = 1;
  boolean hasRequestId();
  long getRequestId();
  
  // required .de.uniluebeck.itm.tr.iwsn.messages.Request.Type type = 2;
  boolean hasType();
  de.uniluebeck.itm.tr.iwsn.messages.Request.Type getType();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest areNodesAliveRequest = 101;
  boolean hasAreNodesAliveRequest();
  de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest getAreNodesAliveRequest();
  de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequestOrBuilder getAreNodesAliveRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.AreNodesConnectedRequest areNodesConnectedRequest = 102;
  boolean hasAreNodesConnectedRequest();
  de.uniluebeck.itm.tr.iwsn.messages.AreNodesConnectedRequest getAreNodesConnectedRequest();
  de.uniluebeck.itm.tr.iwsn.messages.AreNodesConnectedRequestOrBuilder getAreNodesConnectedRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.DisableNodesRequest disableNodesRequest = 103;
  boolean hasDisableNodesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.DisableNodesRequest getDisableNodesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.DisableNodesRequestOrBuilder getDisableNodesRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.DisableVirtualLinksRequest disableVirtualLinksRequest = 104;
  boolean hasDisableVirtualLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.DisableVirtualLinksRequest getDisableVirtualLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.DisableVirtualLinksRequestOrBuilder getDisableVirtualLinksRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.DisablePhysicalLinksRequest disablePhysicalLinksRequest = 105;
  boolean hasDisablePhysicalLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.DisablePhysicalLinksRequest getDisablePhysicalLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.DisablePhysicalLinksRequestOrBuilder getDisablePhysicalLinksRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.EnableNodesRequest enableNodesRequest = 106;
  boolean hasEnableNodesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.EnableNodesRequest getEnableNodesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.EnableNodesRequestOrBuilder getEnableNodesRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.EnablePhysicalLinksRequest enablePhysicalLinksRequest = 107;
  boolean hasEnablePhysicalLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.EnablePhysicalLinksRequest getEnablePhysicalLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.EnablePhysicalLinksRequestOrBuilder getEnablePhysicalLinksRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.EnableVirtualLinksRequest enableVirtualLinksRequest = 108;
  boolean hasEnableVirtualLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.EnableVirtualLinksRequest getEnableVirtualLinksRequest();
  de.uniluebeck.itm.tr.iwsn.messages.EnableVirtualLinksRequestOrBuilder getEnableVirtualLinksRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.FlashImagesRequest flashImagesRequest = 109;
  boolean hasFlashImagesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.FlashImagesRequest getFlashImagesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.FlashImagesRequestOrBuilder getFlashImagesRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.ResetNodesRequest resetNodesRequest = 110;
  boolean hasResetNodesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.ResetNodesRequest getResetNodesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.ResetNodesRequestOrBuilder getResetNodesRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.SendDownstreamMessagesRequest sendDownstreamMessagesRequest = 111;
  boolean hasSendDownstreamMessagesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.SendDownstreamMessagesRequest getSendDownstreamMessagesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.SendDownstreamMessagesRequestOrBuilder getSendDownstreamMessagesRequestOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.SetChannelPipelinesRequest setChannelPipelinesRequest = 112;
  boolean hasSetChannelPipelinesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.SetChannelPipelinesRequest getSetChannelPipelinesRequest();
  de.uniluebeck.itm.tr.iwsn.messages.SetChannelPipelinesRequestOrBuilder getSetChannelPipelinesRequestOrBuilder();
}