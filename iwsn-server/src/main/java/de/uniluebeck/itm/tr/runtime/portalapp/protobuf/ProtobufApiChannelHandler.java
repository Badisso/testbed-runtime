package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService;
import de.uniluebeck.itm.tr.runtime.portalapp.WSNServiceHandle;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppBackendNotifications;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppDownstreamMessage;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppUpstreamMessage;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

import static com.google.common.base.Preconditions.checkArgument;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;

public class ProtobufApiChannelHandler extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(ProtobufApiChannelHandler.class.getName());

	private boolean firstMessage = true;

	private final ProtobufApiService protobufApiService;

	private final SessionManagementService sessionManagement;

	private Channel channel;

	private WSNServiceHandle wsnServiceHandle;

	private final Service.Listener wsnServiceLifecycleListener = new Service.Listener() {

		@Override
		public void starting() {
			// nothing to do
		}

		@Override
		public void running() {
			// nothing to do
		}

		@Override
		public void stopping(final Service.State from) {

			if (channel != null) {

				// TODO send notification to client
				channel.close().addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(final ChannelFuture future) throws Exception {
						if (future.isSuccess()) {
							log.debug("Successfully closed client channel!");
						} else {
							log.error("Exception while closing client channel: {}", future.getCause());
						}
					}
				}
				);
			}
		}

		@Override
		public void terminated(final Service.State from) {
			// nothing to do
		}

		@Override
		public void failed(final Service.State from, final Throwable failure) {
			// nothing to do
		}

	};

	public ProtobufApiChannelHandler(final ProtobufApiService protobufApiService,
									 final SessionManagementService sessionManagement) {

		this.protobufApiService = protobufApiService;
		this.sessionManagement = sessionManagement;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

		log.debug("Client connected: {}", e);

		channel = e.getChannel();
		protobufApiService.getClientChannels().add(channel);

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

		log.debug("Client disconnected: {}", e);

		if (wsnServiceHandle != null) {
			wsnServiceHandle.getWsnApp().getEventBus().unregister(this);
		}

		channel = null;
		protobufApiService.getClientChannels().remove(channel);

		super.channelDisconnected(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

		WisebedMessages.Envelope envelope = (WisebedMessages.Envelope) e.getMessage();

		switch (envelope.getBodyType()) {
			case SECRET_RESERVATION_KEYS:
				receivedSecretReservationKeys(ctx, e, envelope);
				break;
			case MESSAGE:
				sendDownstreamMessageToNodes(ctx, e, envelope);
				break;
			default:
				log.warn("Received message other than secret reservation keys which is not allowed.");
				e.getChannel().close();
				break;
		}

	}

	@Subscribe
	void onBackendNotification(final WSNAppBackendNotifications notifications) {
		// TODO send to client
	}

	@Subscribe
	void onUpstreamMessage(final WSNAppUpstreamMessage message) {
		// TODO send to client
	}

	private void sendDownstreamMessageToNodes(final ChannelHandlerContext ctx, final MessageEvent e,
											  final WisebedMessages.Envelope envelope) {

		log.debug("ProtobufApiChannelHandler.sendDownstreamMessageToNodes({}, {}, {})", new Object[]{ctx, e, envelope});

		if (firstMessage) {
			log.warn("Received message before receiving secret reservation keys. Closing channel: {}", channel);
			final String message = "Received message before receiving secret reservation keys. Closing channel.";
			sendBackendNotificationToClient(ctx, e.getRemoteAddress(), message);
			ctx.getChannel().close();
			return;
		}

		final WisebedMessages.Message message = envelope.getMessage();
		final ImmutableSet<String> to = ImmutableSet.copyOf(message.getNodeBinary().getDestinationNodeUrnsList());
		final byte[] messageBytes = message.getNodeBinary().getData().toByteArray();

		if (log.isDebugEnabled()) {
			log.debug("Sending message {} to nodeUrns {}", toPrintableString(messageBytes, 200), to);
		}

		wsnServiceHandle.getWsnApp().getEventBus().post(new WSNAppDownstreamMessage(to, messageBytes));
	}

	private void receivedSecretReservationKeys(final ChannelHandlerContext ctx, final MessageEvent e,
											   final WisebedMessages.Envelope envelope) {

		log.debug("ProtobufApiChannelHandler.receivedSecretReservationKeys()");

		if (firstMessage) {
			firstMessage = false;
		}

		checkArgument(!firstMessage, "Secret reservation keys are only allowed to be sent as the very first message.");
		checkArgument(envelope.hasSecretReservationKeys(), "Envelope is missing secret reservation keys");

		final String secretReservationKey = envelope.getSecretReservationKeys().getKeys(0).getKey();
		wsnServiceHandle = sessionManagement.getWsnServiceHandle(secretReservationKey);

		if (wsnServiceHandle == null) {

			log.debug("Received reservation key for unknown started WSN instance. Trying to start instance...");

			try {

				sessionManagement.getInstance(secretReservationKey, "NONE");
				wsnServiceHandle = sessionManagement.getWsnServiceHandle(secretReservationKey);

			} catch (Exception e1) {

				log.debug("Exception while trying to get WSN instance: ", e1);
				final String message = "Exception while trying to get WSN instance: " + e1;
				sendBackendNotificationToClient(ctx, e.getRemoteAddress(), message);
				channel.close();
				return;
			}

			if (wsnServiceHandle == null) {

				final String message = "Invalid secret reservation key. Closing channel.";
				log.debug(message);
				sendBackendNotificationToClient(ctx, e.getRemoteAddress(), message);
				channel.close();
				return;
			}
		}

		log.debug("Valid secret reservation key. Starting to listen for messages...");

		wsnServiceHandle.getWsnApp().getEventBus().register(this);
		wsnServiceHandle.getWsnService().addListener(wsnServiceLifecycleListener, MoreExecutors.sameThreadExecutor());
	}

	private void sendBackendNotificationToClient(final ChannelHandlerContext ctx, final SocketAddress remoteAddress,
												 final String text) {

		DefaultChannelFuture future = new DefaultChannelFuture(ctx.getChannel(), true);
		future.addListener(createLoggingListener(remoteAddress, text));
		Channels.write(ctx, future, createNotificationMessage(text));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {

		log.warn("Unexpected exception from downstream: {}", e);
		e.getChannel().close();
	}

	private static ChannelFutureListener createLoggingListener(final SocketAddress remoteAddress, final String text) {
		return new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					//noinspection ThrowableResultOfMethodCallIgnored
					log.warn(
							"Delivery of backend notification ({}) to {} was not successful. Reason: {}",
							new Object[]{text, remoteAddress, future.getCause()}
					);
				} else if (future.isCancelled()) {
					log.warn("Delivery of backend notification ({}) to {} was cancelled.", text, remoteAddress);
				} else if (future.isSuccess()) {
					log.trace("Delivery of backend message ({}) to {} successful.", text, remoteAddress);
				}
			}
		};
	}

	private static WisebedMessages.Envelope createNotificationMessage(final String text) {

		WisebedMessages.Message.Backend.Builder backendBuilder = WisebedMessages.Message.Backend.newBuilder()
				.setText(text);

		WisebedMessages.Message.Builder messageBuilder = WisebedMessages.Message.newBuilder()
				.setType(WisebedMessages.Message.Type.BACKEND)
				.setBackend(backendBuilder);

		return WisebedMessages.Envelope.newBuilder()
				.setMessage(messageBuilder)
				.setBodyType(WisebedMessages.Envelope.BodyType.MESSAGE)
				.build();
	}
}