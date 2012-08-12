package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.newoverlay.MessageUpstreamRequest;
import de.uniluebeck.itm.tr.util.StringUtils;
import eu.wisebed.api.WisebedServiceHelper;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.wsn.WSN;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

public class WSNServiceVirtualLinkManager extends AbstractService {

	private class DeliverVirtualLinkMessageCallable implements Callable<Void> {

		private String targetNode;

		private WSN recipient;

		private Message message;

		private int tries = 0;

		public DeliverVirtualLinkMessageCallable(final String targetNode,
												 final WSN recipient,
												 final Message message) {
			this.targetNode = targetNode;
			this.recipient = recipient;
			this.message = message;
		}

		@Override
		public Void call() throws Exception {

			if (tries < 3) {

				tries++;

				log.debug("Delivering virtual link message to remote testbed service.");

				try {

					recipient.send(Arrays.asList(targetNode), message);

				} catch (Exception e) {

					if (tries >= 3) {

						log.warn("Repeatedly couldn't deliver virtual link message. Dropping message.");

					} else {

						log.warn("Error while delivering virtual link message to remote testbed service. "
								+ "Trying again in 5 seconds."
						);
						scheduledExecutorService.schedule(this, 5, TimeUnit.SECONDS);
					}
				}
			}

			return null;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(WSNServiceVirtualLinkManager.class);

	private static final byte MESSAGE_TYPE_PLOT = 105;

	private static final byte MESSAGE_TYPE_WISELIB_DOWNSTREAM = 10;

	private static final byte NODE_OUTPUT_VIRTUAL_LINK = 52;

	private static final byte WISELIB_VIRTUAL_LINK_MESSAGE = 11;

	private static DatatypeFactory DATATYPE_FACTORY;

	static {
		try {
			DATATYPE_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw propagate(e);
		}
	}

	/**
	 * Map: (Source Node URN) -> (Map: (Target Node URN) -> (WSN endpoint instance))
	 */
	private ImmutableMap<String, ImmutableMap<String, WSN>> virtualLinksMap = ImmutableMap.of();

	private final EventBus eventBus;

	private final ScheduledExecutorService scheduledExecutorService;

	@Inject
	WSNServiceVirtualLinkManager(final EventBus eventBus,
								 final ScheduledExecutorService scheduledExecutorService) {

		this.eventBus = checkNotNull(eventBus);
		this.scheduledExecutorService = checkNotNull(scheduledExecutorService);
	}

	@Override
	protected void doStart() {
		try {
			eventBus.register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Override
	protected void doStop() {
		try {
			eventBus.unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	public void onMessageUpstreamRequest(final MessageUpstreamRequest request) {

		log.trace("WSNServiceVirtualLinkManager.onMessageUpstreamRequest({})", request);

		// check if message is a virtual link message
		final byte[] messageBytes = request.getMessageBytes();

		boolean isVirtualLinkMessage = messageBytes.length > 1 &&
				messageBytes[0] == MESSAGE_TYPE_PLOT &&
				messageBytes[1] == NODE_OUTPUT_VIRTUAL_LINK;

		if (isVirtualLinkMessage) {
			deliverVirtualLinkMessage(messageBytes, request.getFrom().toString(), request.getTimestamp());
		}
	}

	public void addVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance) {

		if (!containsVirtualLink(sourceNode, targetNode)) {

			log.debug("+++ Adding virtual link from {} to {}", sourceNode, targetNode);

			WSN remoteServiceEndpoint = WisebedServiceHelper.getWSNService(remoteServiceInstance);

			//Create a new immutable map with this sourceNode and all existing <targetNode, WSN> mappings
			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();

			//Add potentially existing <targetNode, WSN> mappings for this source node to the new list
			if (virtualLinksMap.get(sourceNode) != null) {
				targetNodeMapBuilder.putAll(virtualLinksMap.get(sourceNode));
			}
			//Add the new <targetNode, WSN> mapping to this new list
			targetNodeMapBuilder.put(targetNode, remoteServiceEndpoint);

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			//We now add all existing source nodes to the map except for the current source node
			//It looks a bit strange but we cannot use putAll and then overwrite an existing key
			//because the ImmutableMapBuilder forbids duplicate keys
			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNode)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNode, targetNodeMapBuilder.build());
			virtualLinksMap = virtualLinksMapBuilder.build();

		} else {
			log.debug("+++ Not adding virtual link from {} to {} as it is already established", sourceNode, targetNode);
		}

	}

	public void removeVirtualLink(final String sourceNode, final String targetNode) {

		if (containsVirtualLink(sourceNode, targetNode)) {

			log.debug("--- Removing virtual link from {} to {}", sourceNode, targetNode);

			ImmutableMap.Builder<String, WSN> targetNodeMapBuilder = ImmutableMap.builder();
			for (Map.Entry<String, WSN> oldEntry : virtualLinksMap.get(sourceNode).entrySet()) {
				if (!targetNode.equals(oldEntry.getKey())) {
					targetNodeMapBuilder.put(oldEntry.getKey(), oldEntry.getValue());
				}
			}

			ImmutableMap.Builder<String, ImmutableMap<String, WSN>> virtualLinksMapBuilder = ImmutableMap.builder();

			for (String existingSourceNode : virtualLinksMap.keySet()) {
				if (!existingSourceNode.equals(sourceNode)) {
					virtualLinksMapBuilder.put(existingSourceNode, virtualLinksMap.get(existingSourceNode));
				}
			}

			virtualLinksMapBuilder.put(sourceNode, targetNodeMapBuilder.build());

			virtualLinksMap = virtualLinksMapBuilder.build();

		}

	}

	private boolean containsVirtualLink(String sourceNode, String targetNode) {
		ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNode);
		return map != null && map.containsKey(targetNode);
	}

	private void deliverVirtualLinkMessage(final byte[] bytes, final String sourceNodeId, final String timestamp) {

		long destinationNode = readDestinationNodeURN(bytes);
		Map<String, WSN> recipients = determineVirtualLinkMessageRecipients(sourceNodeId, destinationNode);

		if (recipients.size() > 0) {

			Message outboundVirtualLinkMessage =
					constructOutboundVirtualLinkMessage(bytes, sourceNodeId, timestamp);

			for (Map.Entry<String, WSN> recipient : recipients.entrySet()) {

				String targetNode = recipient.getKey();
				WSN recipientEndpointProxy = recipient.getValue();

				scheduledExecutorService.submit(
						new DeliverVirtualLinkMessageCallable(
								targetNode,
								recipientEndpointProxy,
								outboundVirtualLinkMessage
						)
				);
			}
		}
	}

	private Message constructOutboundVirtualLinkMessage(final byte[] bytes, final String sourceNodeId,
														final String timestamp) {

		// byte 0: ISense Packet Type
		// byte 1: Node API Command Type
		// byte 2: RSSI
		// byte 3: LQI
		// byte 4: Payload Length
		// byte 5-8: Destination Node URN
		// byte 9-12: Source Node URN
		// byte 13-13+Payload Length: Payload

		Message outboundVirtualLinkMessage = new Message();
		outboundVirtualLinkMessage.setSourceNodeId(sourceNodeId);
		outboundVirtualLinkMessage.setTimestamp(DATATYPE_FACTORY.newXMLGregorianCalendar(timestamp));

		// construct message that is actually sent to the destination node URN
		ChannelBuffer header = ChannelBuffers.buffer(3);
		header.writeByte(MESSAGE_TYPE_WISELIB_DOWNSTREAM);
		header.writeByte(WISELIB_VIRTUAL_LINK_MESSAGE);
		header.writeByte(0); // request id according to Node API

		ChannelBuffer payload = ChannelBuffers.wrappedBuffer(bytes, 2, bytes.length - 2);
		ChannelBuffer packet = ChannelBuffers.wrappedBuffer(header, payload);

		byte[] outboundVirtualLinkMessageBinaryData = new byte[packet.readableBytes()];
		packet.getBytes(0, outboundVirtualLinkMessageBinaryData);

		outboundVirtualLinkMessage.setBinaryData(outboundVirtualLinkMessageBinaryData);

		return outboundVirtualLinkMessage;
	}

	private Map<String, WSN> determineVirtualLinkMessageRecipients(final String sourceNodeURN,
																   final long destinationNode) {

		// check if message is a broadcast or unicast message
		boolean isBroadcast = destinationNode == 0xFFFF;

		// send virtual link message to all recipients
		Map<String, WSN> recipients = new HashMap<String, WSN>();

		if (isBroadcast) {

			ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeURN);
			if (map != null) {
				for (Map.Entry<String, WSN> entry : map.entrySet()) {
					recipients.put(entry.getKey(), entry.getValue());
				}
			}

		} else {

			ImmutableMap<String, WSN> map = virtualLinksMap.get(sourceNodeURN);
			for (String targetNode : map.keySet()) {

				if (StringUtils.parseHexOrDecLongFromUrn(targetNode) == destinationNode) {
					recipients.put(targetNode, map.get(targetNode));
				}
			}
		}

		return recipients;
	}

	private long readDestinationNodeURN(final byte[] virtualLinkMessage) {
		ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(virtualLinkMessage);
		return buffer.getLong(5);
	}

}
