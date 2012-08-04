package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;
import org.joda.time.DateTime;

public interface RequestFactory {

	AreNodesAliveRequest createAreNodesAliveRequest(ImmutableSet<NodeUrn> nodeUrns);

	AreNodesAliveSmRequest createAreNodesAliveSmRequest(ImmutableSet<NodeUrn> nodeUrns);

	DestroyVirtualLinkRequest createDestroyVirtualLinkRequest(@Assisted("from") NodeUrn from,
															  @Assisted("to") NodeUrn to);

	DisableNodeRequest createDisableNodeRequest(NodeUrn nodeUrn);

	DisablePhysicalLinkRequest createDisablePhysicalLinkRequest(@Assisted("from") NodeUrn from,
																@Assisted("to") NodeUrn to);

	EnableNodeRequest createEnableNodeRequest(NodeUrn nodeUrn);

	EnablePhysicalLinkRequest createEnablePhysicalLinkRequest(@Assisted("from") NodeUrn from,
															  @Assisted("to") NodeUrn to);

	FlashDefaultImageRequest createFlashDefaultImageRequest(ImmutableSet<NodeUrn> nodeUrns);

	FlashImageRequest createFlashImageRequest(ImmutableMap<ImmutableSet<NodeUrn>, byte[]> images);

	ResetNodesRequest createResetNodesRequest(ImmutableSet<NodeUrn> nodeUrns);

	DevicesAttachedEventRequest createDevicesAttachedEventRequest(ImmutableSet<NodeUrn> nodeUrns);

	DevicesDetachedEventRequest createDevicesDetachedEventRequest(ImmutableSet<NodeUrn> nodeUrns);

	MessageDownstreamRequest createMessageDownstreamRequest(ImmutableSet<NodeUrn> to, byte[] messageBytes);

	MessageUpstreamRequest createMessageUpstreamRequest(NodeUrn from, DateTime timestamp, byte[] messageBytes);

	SetChannelPipelineRequest createSetChannelPipelineRequest(ImmutableSet<NodeUrn> nodeUrns,
															  ImmutableList<Tuple<String, ImmutableMap<String, String>>> pipeline);

	SetDefaultChannelPipelineRequest createSetDefaultChannelPipelineRequest(ImmutableSet<NodeUrn> nodeUrns);

	SetVirtualLinkRequest createSetVirtualLinkRequest(@Assisted("from") NodeUrn from,
													  @Assisted("to") NodeUrn to);

}
