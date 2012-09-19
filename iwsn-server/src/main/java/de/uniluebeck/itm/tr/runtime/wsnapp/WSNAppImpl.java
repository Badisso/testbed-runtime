/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.MessageTools;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.event.MessageEventListener;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.srmr.SingleRequestMultiResponseCallback;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.unreliable.UnknownNameException;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.tr.runtime.wsnapp.pipeline.AbovePipelineLogger;
import de.uniluebeck.itm.tr.runtime.wsnapp.pipeline.BelowPipelineLogger;
import de.uniluebeck.itm.tr.runtime.wsnapp.pipeline.EmbeddedChannel;
import de.uniluebeck.itm.tr.runtime.wsnapp.pipeline.EmbeddedChannelSink;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.TimeDiff;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.runtime.wsnapp.pipeline.PipelineHelper.*;
import static de.uniluebeck.itm.tr.util.StringUtils.toPrintableString;
import static org.jboss.netty.channel.Channels.future;
import static org.jboss.netty.channel.Channels.pipeline;


class WSNAppImpl extends AbstractService implements WSNApp {

	private static class RequestStatusRunnable implements Runnable {

		private final ListenableFuture<byte[]> future;

		private final Callback callback;

		private final String nodeUrn;

		private final long instantiation;

		private RequestStatusRunnable(final ListenableFuture<byte[]> future, final Callback callback,
									  final String nodeUrn) {

			this.future = future;
			this.callback = callback;
			this.nodeUrn = nodeUrn;

			this.instantiation = System.currentTimeMillis();
		}

		@Override
		public void run() {
			try {
				success(future.get());
			} catch (Exception e) {
				failure(e);
			}
		}

		private void success(byte[] reply) {
			try {

				WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
						.newBuilder()
						.mergeFrom(reply)
						.build();

				log.debug("Received reply after {} milliseconds from {}.",
						(System.currentTimeMillis() - instantiation), nodeUrn
				);

				callback.receivedRequestStatus(requestStatus);

			} catch (InvalidProtocolBufferException e) {
				callbackError("Internal error occurred while delivering message...", -2);
			}
		}

		private void failure(Exception exception) {

			String message = "Exception after "
					+ (System.currentTimeMillis() - instantiation)
					+ "ms while executing operation: "
					+ exception.getMessage() + "\n"
					+ Throwables.getStackTraceAsString(exception);

			log.debug(message);
			callbackError(message, -1);
		}

		private void callbackError(String msg, int code) {

			WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
					.newBuilder()
					.setNodeUrn(nodeUrn)
					.setMsg(msg)
					.setValue(code)
					.build();

			log.debug("Received error after {} milliseconds from {}.",
					(System.currentTimeMillis() - instantiation),
					nodeUrn
			);

			callback.receivedRequestStatus(requestStatus);

		}
	}

	private final SimpleChannelUpstreamHandler filterPipelineTopHandler = new SimpleChannelUpstreamHandler() {

		@Override
		public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

			ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();
			SocketAddress socketAddress = e.getRemoteAddress();

			byte[] bytes = new byte[channelBuffer.readableBytes()];
			channelBuffer.readBytes(bytes);

			String sourceNodeId = ((WisebedMulticastAddress) socketAddress).getNodeUrns().iterator().next();
			String timestamp = (String) ((WisebedMulticastAddress) socketAddress).getUserContext().get("timestamp");

			if (log.isTraceEnabled()) {
				log.trace(
						"{} => {}",
						new Object[]{sourceNodeId, StringUtils.toHexString(bytes)}
				);
			}

			eventBus.post(new WSNAppUpstreamMessage(sourceNodeId, timestamp, bytes));
		}

		@Override
		public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
			sendBackendNotificationsToUser("Exception in channel pipeline caught: " + e);
		}
	};

	private static final Logger log = LoggerFactory.getLogger(WSNApp.class);

	private TestbedRuntime testbedRuntime;

	private ImmutableSet<String> reservedNodes;

	private HandlerFactoryRegistry handlerFactoryRegistry;

	private static final int PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE = 5000;

	private final AbovePipelineLogger abovePipelineLogger;

	private final BelowPipelineLogger belowPipelineLogger;

	private final TimeDiff pipelineMisconfigurationTimeDiff = new TimeDiff(PIPELINE_MISCONFIGURATION_NOTIFICATION_RATE);

	private ScheduledExecutorService scheduler;

	private ExecutorService executor;

	private final ChannelPipeline pipeline = pipeline();

	@SuppressWarnings("unused")
	private final Channel channel = new EmbeddedChannel(pipeline, new EmbeddedChannelSink());

	private final WSNAppEventBus eventBus;

	private MessageEventListener messageEventListener = new MessageEventAdapter() {

		@Override
		public void messageReceived(Messages.Msg msg) {

			boolean fromReservedNode = reservedNodes.contains(msg.getFrom());

			if (fromReservedNode) {

				boolean isMessage = WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(msg.getMsgType());
				boolean isNotification = WSNApp.MSG_TYPE_LISTENER_NOTIFICATION.equals(msg.getMsgType());

				if (isMessage) {
					deliverMessage(msg);
				} else if (isNotification) {
					deliverNotification(msg);
				}
			}

		}

		private void deliverMessage(final Messages.Msg msg) {

			try {

				WSNAppMessages.UpstreamMessage message = WSNAppMessages.UpstreamMessage.newBuilder()
						.mergeFrom(msg.getPayload())
						.build();

				if (log.isDebugEnabled()) {
					String output = WSNAppMessageTools.toString(message, true, 200);
					output = output.endsWith("\n") ? output.substring(0, output.length() - 2) : output;
					log.debug("{}", output);
				}

				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(message.getMessageBytes().toByteArray());

				final Map<String, Object> userContext = Maps.newHashMap();
				userContext.put("timestamp", message.getTimestamp());

				final WisebedMulticastAddress sourceAddress = new WisebedMulticastAddress(
						newHashSet(message.getSourceNodeUrn()),
						userContext
				);

				final UpstreamMessageEvent upstreamMessageEvent = new UpstreamMessageEvent(
						pipeline.getChannel(),
						buffer,
						sourceAddress
				);
				pipeline.sendUpstream(upstreamMessageEvent);

			} catch (InvalidProtocolBufferException e) {
				log.error("" + e, e);
			}
		}

		private void deliverNotification(final Messages.Msg msg) {

			try {

				WSNAppMessages.Notification notification = WSNAppMessages.Notification.newBuilder()
						.mergeFrom(msg.getPayload())
						.build();

				sendBackendNotificationsToUser(notification.getMsg());

			} catch (InvalidProtocolBufferException e) {
				log.error("" + e, e);
			}

		}
	};

	@Inject
	WSNAppImpl(final WSNAppEventBus eventBus,
			   @Assisted final TestbedRuntime testbedRuntime,
			   @Assisted final ImmutableSet<String> reservedNodes) {

		this.eventBus = eventBus;
		this.testbedRuntime = testbedRuntime;
		this.reservedNodes = reservedNodes;

		this.handlerFactoryRegistry = new HandlerFactoryRegistry();
		ProtocolCollection.registerProtocols(this.handlerFactoryRegistry);

		abovePipelineLogger = new AbovePipelineLogger();
		belowPipelineLogger = new BelowPipelineLogger();
	}

	private String getLocalNodeName() {
		return testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().iterator().next();
	}

	private final SimpleChannelDownstreamHandler filterPipelineBottomHandler = new SimpleChannelDownstreamHandler() {

		@Override
		public void handleDownstream(final ChannelHandlerContext ctx, final ChannelEvent e) throws Exception {

			if (e instanceof ExceptionEvent) {

				@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
				Throwable cause = ((ExceptionEvent) e).getCause();

				String notificationString = "The pipeline seems to be wrongly configured. A(n) " +
						cause.getClass().getSimpleName() +
						" was caught and contained the following message: " +
						cause.getMessage();

				sendPipelineMisconfigurationIfNotificationRateAllows(notificationString);

			} else if (e instanceof MessageEvent) {

				writeRequested(ctx, (MessageEvent) e);
			}
		}

		@Override
		public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

			ChannelBuffer buf = (ChannelBuffer) e.getMessage();
			SocketAddress socketAddress = e.getRemoteAddress();

			Set<String> nodeUrns;
			final Callback callback;

			if (socketAddress instanceof WisebedMulticastAddress) {

				nodeUrns = ((WisebedMulticastAddress) socketAddress).getNodeUrns();
				callback = (Callback) ((WisebedMulticastAddress) socketAddress).getUserContext().get("callback");

			} else {
				throw new RuntimeException(
						"Expected type " + WisebedMulticastAddress.class.getName() + "but got " + socketAddress
								.getClass()
								.getName() + "!"
				);
			}

			for (final String nodeUrn : nodeUrns) {

				final WSNAppMessages.DownstreamMessage.Builder message = WSNAppMessages.DownstreamMessage
						.newBuilder()
						.setMessageBytes(ByteString.copyFrom(buf.array(), buf.readerIndex(), buf.readableBytes()))
						.addTargetNodeUrns(nodeUrn);

				final WSNAppMessages.Invocation operationInvocation = WSNAppMessages.Invocation
						.newBuilder()
						.setType(WSNAppMessages.Invocation.Type.SEND_DOWNSTREAM)
						.setDownstreamMessage(message)
						.build();

				try {

					final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
							getLocalNodeName(),
							nodeUrn,
							MSG_TYPE_OPERATION_INVOCATION_REQUEST,
							operationInvocation.toByteArray(),
							UnreliableMessagingService.PRIORITY_NORMAL,
							10, TimeUnit.SECONDS
					);

					future.addListener(new Runnable() {

						@Override
						public void run() {
							try {

								future.get();

								WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
										.newBuilder()
										.setNodeUrn(nodeUrn)
										.setValue(1)
										.build();

								callback.receivedRequestStatus(requestStatus);

							} catch (Exception e) {
								callback.failure(e);
							}
						}

					}, executor
					);

				} catch (UnknownNameException e1) {

					WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
							.newBuilder()
							.setNodeUrn(nodeUrn)
							.setMsg("Unknown node URN \"" + nodeUrn + "\"")
							.setValue(-1)
							.build();

					callback.receivedRequestStatus(requestStatus);
				}
			}
		}
	};

	private void sendPipelineMisconfigurationIfNotificationRateAllows(String notificationString) {

		if (pipelineMisconfigurationTimeDiff.isTimeout()) {
			sendBackendNotificationsToUser(notificationString);
			pipelineMisconfigurationTimeDiff.touch();
		}
	}

	@Override
	public String getName() {
		return WSNApp.class.getSimpleName();
	}

	@Override
	protected void doStart() {

		try {

			final ThreadFactory schedulerThreadFactory = new ThreadFactoryBuilder()
					.setNameFormat("WSNApp-Thread %d")
					.build();
			scheduler = Executors.newScheduledThreadPool(1, schedulerThreadFactory);

			final ThreadFactory executorThreadFactory = new ThreadFactoryBuilder()
					.setNameFormat("WSNApp-Executor-Thread %d")
					.build();
			executor = Executors.newCachedThreadPool(executorThreadFactory);

			setDefaultPipelineLocally();

			// start listening to sensor node output messages
			testbedRuntime.getMessageEventService().addListener(messageEventListener);

			eventBus.register(this);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStarted();
	}

	@Override
	protected void doStop() {

		try {

			log.info("Stopping WSNApp...");

			eventBus.unregister(this);

			setDefaultPipelineOnReservedNodes();
			setDefaultPipelineLocally();
			flashDefaultImageToReservedNodes();

			// stop listening for messages from the nodes
			testbedRuntime.getMessageEventService().removeListener(messageEventListener);

			ExecutorUtils.shutdown(executor, 1, TimeUnit.SECONDS);
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);

			log.info("WSNApp stopped!");

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Subscribe
	public void onDownstreamMessage(final WSNAppDownstreamMessage message) {

		log.debug("WSNAppImpl.onDownstreamMessage({})", message);

		try {
			assertNodeUrnsKnown(message.getTo());
		} catch (UnknownNodeUrnsException e) {
			for (String nodeUrn : message.getTo()) {
				if (e.getNodeUrns().contains(nodeUrn)) {
					message.getFutureMap().get(nodeUrn).setException(e);
				} else {
					message.getFutureMap().get(nodeUrn).setException(new Exception("Cancelled"));
				}
			}
			return;
		}

		final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(message.getMessageBytes());
		final WisebedMulticastAddress targetAddress = new WisebedMulticastAddress(message.getTo());
		targetAddress.getUserContext().put("callback", new Callback() {

			@Override
			public void receivedRequestStatus(final WSNAppMessages.RequestStatus requestStatus) {

				final String nodeUrn = requestStatus.getNodeUrn();

				if (requestStatus.getValue() >= 1) {

					message.getFutureMap().get(nodeUrn).set(null);

				} else if (requestStatus.getValue() < 0) {

					final Exception exception = new Exception(requestStatus.getValue() + ": " + requestStatus.getMsg());
					message.getFutureMap().get(nodeUrn).setException(exception);
				}
			}

			@Override
			public void failure(final Exception e) {
				for (SettableFuture<Void> future : message.getFutureMap().values()) {
					future.setException(e);
				}
			}
		}
		);

		final DownstreamMessageEvent downstreamMessageEvent = new DownstreamMessageEvent(
				pipeline.getChannel(),
				future(pipeline.getChannel()),
				buffer,
				targetAddress
		);

		pipeline.sendDownstream(downstreamMessageEvent);

		for (SettableFuture<Void> future : message.getFutureMap().values()) {
			future.set(null);
		}
	}

	private void sendBackendNotificationsToUser(final String... messages) {
		eventBus.post(new WSNAppBackendNotifications(ImmutableList.copyOf(messages)));
	}

	private void setDefaultPipelineLocally() {
		setChannelPipelineLocally(null, null);
	}

	@Override
	public void setChannelPipeline(final Set<String> nodeUrns,
								   final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
								   final Callback callback) throws UnknownNodeUrnsException {

		boolean filterLocallyAtPortal = nodeUrns.isEmpty();

		if (filterLocallyAtPortal) {
			setChannelPipelineLocally(channelHandlerConfigurations, callback);
		} else {
			setChannelPipelineOnGateways(nodeUrns, channelHandlerConfigurations, callback);
		}
	}

	private void setChannelPipelineLocally(
			@Nullable final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
			@Nullable final Callback callback) {

		try {

			final List<Tuple<String, ChannelHandler>> innerPipelineHandlers = channelHandlerConfigurations != null ?
					handlerFactoryRegistry.create(convertCHCList(channelHandlerConfigurations)) :
					null;

			final List<Tuple<String, ChannelHandler>> pipelineHandlers = createPipelineHandlers(innerPipelineHandlers);

			if (log.isDebugEnabled()) {
				log.debug("Setting pipeline to: {}", Arrays.toString(pipelineHandlers.toArray()));
			}

			setPipeline(pipeline, pipelineHandlers);

			if (callback != null) {

				final WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
						.newBuilder()
						.setValue(1)
						.setNodeUrn("")
						.build();

				callback.receivedRequestStatus(requestStatus);
			}

		} catch (Exception e) {

			log.warn("Exception while setting channel pipeline on portal host: " + e, e);

			setPipeline(pipeline, createPipelineHandlers(null));

			if (callback != null) {
				callback.failure(e);
			}
		}
	}

	@Nonnull
	private List<Tuple<String, ChannelHandler>> createPipelineHandlers(
			@Nullable List<Tuple<String, ChannelHandler>> innerHandlers) {

		LinkedList<Tuple<String, ChannelHandler>> handlers = newLinkedList();

		handlers.addFirst(new Tuple<String, ChannelHandler>("filterPipelineTopHandler", filterPipelineTopHandler));
		handlers.addFirst(new Tuple<String, ChannelHandler>("aboveFilterPipelineLogger", abovePipelineLogger));

		if (innerHandlers != null) {
			for (Tuple<String, ChannelHandler> innerHandler : innerHandlers) {
				handlers.addFirst(innerHandler);
			}
		}

		handlers.addFirst(new Tuple<String, ChannelHandler>("belowFilterPipelineLogger", belowPipelineLogger));
		handlers.addFirst(new Tuple<String, ChannelHandler>("filterPipelineBottomHandler", filterPipelineBottomHandler)
		);

		return handlers;
	}

	private void setChannelPipelineOnGateways(final Set<String> nodeUrns,
											  final List<ChannelHandlerConfiguration> channelHandlerConfigurations,
											  final Callback callback) throws UnknownNodeUrnsException {
		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.Invocation.Builder operationInvocation = WSNAppMessages.Invocation
				.newBuilder()
				.setSetChannelPipelineRequest(convertToProtobuf(channelHandlerConfigurations))
				.setType(WSNAppMessages.Invocation.Type.SET_CHANNEL_PIPELINE);

		final byte[] bytes = operationInvocation.build().toByteArray();

		for (String nodeUrn : nodeUrns) {

			try {
				final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
						getLocalNodeName(),
						nodeUrn,
						MSG_TYPE_OPERATION_INVOCATION_REQUEST,
						bytes,
						UnreliableMessagingService.PRIORITY_NORMAL,
						10, TimeUnit.SECONDS
				);
				future.addListener(new RequestStatusRunnable(future, callback, nodeUrn), executor);
			} catch (UnknownNameException e) {
				callback.failure(e);
			}
		}
	}

	@Override
	public WSNAppEventBus getEventBus() {
		return eventBus;
	}

	@Override
	public void areNodesAlive(final Set<String> nodeUrns, final Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.Invocation.Builder builder = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.ARE_NODES_ALIVE);

		byte[] bytes = builder.build().toByteArray();

		if (log.isDebugEnabled()) {
			log.debug("Sending checkAreNodesAlive operation invocation, bytes: {}", toPrintableString(bytes, 200));
		}

		for (String nodeUrn : nodeUrns) {
			final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
					getLocalNodeName(),
					nodeUrn,
					MSG_TYPE_OPERATION_INVOCATION_REQUEST,
					bytes,
					UnreliableMessagingService.PRIORITY_NORMAL,
					10, TimeUnit.SECONDS
			);
			future.addListener(new RequestStatusRunnable(future, callback, nodeUrn), executor);
		}
	}

	@Override
	public void areNodesAliveSm(final Set<String> nodeUrns, final Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.Invocation.Builder builder = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.ARE_NODES_CONNECTED);

		byte[] bytes = builder.build().toByteArray();

		if (log.isDebugEnabled()) {
			log.debug("Sending checkAreNodesAliveSm operation invocation, bytes: {}", toPrintableString(bytes, 200));
		}

		for (String nodeUrn : nodeUrns) {
			final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
					getLocalNodeName(),
					nodeUrn,
					MSG_TYPE_OPERATION_INVOCATION_REQUEST,
					bytes,
					UnreliableMessagingService.PRIORITY_NORMAL,
					10, TimeUnit.SECONDS
			);
			future.addListener(new RequestStatusRunnable(future, callback, nodeUrn), executor);
		}

	}

	@Override
	public void flashPrograms(final Map<String, byte[]> programs, final Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(programs.keySet());

		WSNAppMessages.Invocation operationInvocationProtobuf = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.FLASH_PROGRAMS)
				.buildPartial();

		for (Map.Entry<String, byte[]> entry : programs.entrySet()) {

			final String nodeUrn = entry.getKey();
			final byte[] image = entry.getValue();

			final WSNAppMessages.FlashImageRequest.Builder flashImageRequest = WSNAppMessages.FlashImageRequest
					.newBuilder()
					.setImage(ByteString.copyFrom(image))
					.addNodeUrns(nodeUrn);

			final WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
					.newBuilder(operationInvocationProtobuf)
					.setFlashImageRequest(flashImageRequest)
					.build();

			final Messages.Msg msg = MessageTools.buildMessage(
					getLocalNodeName(),
					nodeUrn,
					MSG_TYPE_OPERATION_INVOCATION_REQUEST,
					invocation.toByteArray(), UnreliableMessagingService.PRIORITY_NORMAL
			);

			SingleRequestMultiResponseCallback multiResponseCallback = new SingleRequestMultiResponseCallback() {
				@Override
				public boolean receive(byte[] response) {
					try {

						WSNAppMessages.RequestStatus requestStatus =
								WSNAppMessages.RequestStatus.newBuilder()
										.mergeFrom(ByteString.copyFrom(response)).build();

						callback.receivedRequestStatus(requestStatus);

						// cancel the job if error or complete
						return requestStatus.getValue() < 0 || requestStatus.getValue() >= 100;

					} catch (InvalidProtocolBufferException e) {
						log.error("Exception while parsing incoming request status: " + e, e);
					}

					return false;
				}

				@Override
				public void timeout() {

					WSNAppMessages.RequestStatus requestStatus =
							WSNAppMessages.RequestStatus.newBuilder()
									.setValue(-1)
									.setMsg("Flash node operation timed out!")
									.setNodeUrn(nodeUrn)
									.build();

					callback.receivedRequestStatus(requestStatus);

				}

				@Override
				public void failure(Exception exception) {
					callback.failure(exception);
				}
			};

			testbedRuntime.getSingleRequestMultiResponseService().sendUnreliableRequestUnreliableResponse(
					msg,
					2, TimeUnit.MINUTES,
					multiResponseCallback
			);

		}
	}

	@Override
	public void resetNodes(final Set<String> nodeUrns, final Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(nodeUrns);

		WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.RESET_NODES)
				.build();

		byte[] bytes = invocation.toByteArray();

		for (String nodeUrn : nodeUrns) {
			final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
					getLocalNodeName(),
					nodeUrn,
					MSG_TYPE_OPERATION_INVOCATION_REQUEST,
					bytes,
					UnreliableMessagingService.PRIORITY_NORMAL,
					10, TimeUnit.SECONDS
			);
			future.addListener(new RequestStatusRunnable(future, callback, nodeUrn), executor);
		}

	}

	@Override
	public void setVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(sourceNodeUrn));

		final WSNAppMessages.Link.Builder link = WSNAppMessages.Link
				.newBuilder()
				.setSourceNodeUrn(sourceNodeUrn)
				.setTargetNodeUrn(targetNodeUrn);

		final WSNAppMessages.SetVirtualLinksRequest.Builder setVirtualLinkRequestBuilder =
				WSNAppMessages.SetVirtualLinksRequest
						.newBuilder()
						.addLinks(link);

		final WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.SET_VIRTUAL_LINK)
				.setSetVirtualLinksRequest(setVirtualLinkRequestBuilder)
				.build();

		byte[] bytes = invocation.toByteArray();

		final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
				getLocalNodeName(),
				sourceNodeUrn,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				bytes,
				UnreliableMessagingService.PRIORITY_NORMAL,
				10, TimeUnit.SECONDS
		);

		future.addListener(new RequestStatusRunnable(future, callback, sourceNodeUrn), executor);
	}

	@Override
	public void destroyVirtualLink(String sourceNodeUrn, String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(sourceNodeUrn));

		final WSNAppMessages.Link.Builder link = WSNAppMessages.Link.newBuilder()
				.setSourceNodeUrn(sourceNodeUrn)
				.setTargetNodeUrn(targetNodeUrn);

		final WSNAppMessages.DestroyVirtualLinksRequest.Builder destroyVirtualLinksRequest =
				WSNAppMessages.DestroyVirtualLinksRequest
						.newBuilder()
						.addLinks(link);

		final WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.DESTROY_VIRTUAL_LINK)
				.setDestroyVirtualLinksRequest(destroyVirtualLinksRequest)
				.build();

		byte[] bytes = invocation.toByteArray();

		final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
				getLocalNodeName(),
				sourceNodeUrn,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				bytes,
				UnreliableMessagingService.PRIORITY_NORMAL,
				10, TimeUnit.SECONDS
		);
		future.addListener(new RequestStatusRunnable(future, callback, sourceNodeUrn), executor);

	}

	@Override
	public void disableNode(final String nodeUrn, Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(nodeUrn));

		WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.DISABLE_NODE)
				.build();

		byte[] bytes = invocation.toByteArray();

		final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
				getLocalNodeName(),
				nodeUrn,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				bytes,
				UnreliableMessagingService.PRIORITY_NORMAL,
				10, TimeUnit.SECONDS
		);
		future.addListener(new RequestStatusRunnable(future, callback, nodeUrn), executor);

	}

	@Override
	public void enableNode(final String nodeUrn, Callback callback) throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(nodeUrn));

		WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.ENABLE_NODE)
				.build();

		byte[] bytes = invocation.toByteArray();

		final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
				getLocalNodeName(),
				nodeUrn,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				bytes,
				UnreliableMessagingService.PRIORITY_NORMAL,
				10, TimeUnit.SECONDS
		);
		future.addListener(new RequestStatusRunnable(future, callback, nodeUrn), executor);
	}

	@Override
	public void enablePhysicalLink(final String sourceNodeUrn, final String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(sourceNodeUrn, targetNodeUrn));

		final WSNAppMessages.Link.Builder link = WSNAppMessages.Link.newBuilder()
				.setSourceNodeUrn(sourceNodeUrn)
				.setTargetNodeUrn(targetNodeUrn);

		final WSNAppMessages.EnablePhysicalLinksRequest enablePhysicalLinksRequest =
				WSNAppMessages.EnablePhysicalLinksRequest
						.newBuilder()
						.addLinks(link)
						.build();

		final WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.ENABLE_PHYSICAL_LINK)
				.setEnablePhysicalLinksRequest(enablePhysicalLinksRequest)
				.build();

		byte[] bytes = invocation.toByteArray();

		final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
				getLocalNodeName(),
				sourceNodeUrn,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				bytes,
				UnreliableMessagingService.PRIORITY_NORMAL,
				10, TimeUnit.SECONDS
		);
		future.addListener(new RequestStatusRunnable(future, callback, sourceNodeUrn), executor);
	}

	@Override
	public void disablePhysicalLink(final String sourceNodeUrn, final String targetNodeUrn, Callback callback)
			throws UnknownNodeUrnsException {

		assertNodeUrnsKnown(Arrays.asList(sourceNodeUrn, targetNodeUrn));

		final WSNAppMessages.Link.Builder link = WSNAppMessages.Link.newBuilder()
				.setSourceNodeUrn(sourceNodeUrn)
				.setTargetNodeUrn(targetNodeUrn);

		final WSNAppMessages.DisablePhysicalLinksRequest.Builder disablePhysicalLinksRequest =
				WSNAppMessages.DisablePhysicalLinksRequest.newBuilder().addLinks(link);

		final WSNAppMessages.Invocation invocation = WSNAppMessages.Invocation.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.DISABLE_PHYSICAL_LINK)
				.setDisablePhysicalLinksRequest(disablePhysicalLinksRequest)
				.build();

		byte[] bytes = invocation.toByteArray();

		final ListenableFuture<byte[]> future = testbedRuntime.getReliableMessagingService().sendAsync(
				getLocalNodeName(),
				sourceNodeUrn,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				bytes,
				UnreliableMessagingService.PRIORITY_NORMAL,
				10, TimeUnit.SECONDS
		);

		future.addListener(new RequestStatusRunnable(future, callback, sourceNodeUrn), executor);
	}

	private void assertNodeUrnsKnown(Collection<String> nodeUrns) throws UnknownNodeUrnsException {

		ImmutableSet<String> localNodeNames = testbedRuntime.getLocalNodeNameManager().getLocalNodeNames();
		ImmutableSet<String> remoteNodeNames = testbedRuntime.getRoutingTableService().getEntries().keySet();
		Set<String> unknownNodeUrns = null;

		for (String nodeUrn : nodeUrns) {
			if (!remoteNodeNames.contains(nodeUrn) && !localNodeNames.contains(nodeUrn)) {
				if (unknownNodeUrns == null) {
					unknownNodeUrns = new HashSet<String>();
				}
				unknownNodeUrns.add(nodeUrn);
			}
		}

		if (unknownNodeUrns != null) {

			String msg =
					"Ignoring request as the following node URNs are unknown: " + Joiner.on(", ").join(unknownNodeUrns);
			throw new UnknownNodeUrnsException(unknownNodeUrns, msg);
		}

	}

	private void flashDefaultImageToReservedNodes() {

		log.info("Flashing default image to reserved nodes...");

		// fork for all nodes
		List<ListenableFuture<Boolean>> futures = newArrayList();
		for (String reservedNode : reservedNodes) {
			futures.add(flashDefaultImageTo(reservedNode));
		}

		// join results to become synchronous
		for (ListenableFuture<Boolean> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				log.error("Exception while flashing default image to reserved nodes: {}", e.getMessage());
			}
		}
	}

	private ListenableFuture<Boolean> flashDefaultImageTo(final String reservedNode) {

		final SettableFuture<Boolean> future = SettableFuture.create();

		final WSNAppMessages.Invocation operationInvocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.FLASH_DEFAULT_IMAGE)
				.build();

		final Messages.Msg msg = MessageTools.buildMessage(
				getLocalNodeName(),
				reservedNode,
				MSG_TYPE_OPERATION_INVOCATION_REQUEST,
				operationInvocation.toByteArray(),
				UnreliableMessagingService.PRIORITY_NORMAL
		);

		final SingleRequestMultiResponseCallback callback = new SingleRequestMultiResponseCallback() {

			@Override
			public boolean receive(byte[] response) {

				try {

					WSNAppMessages.RequestStatus requestStatus = WSNAppMessages.RequestStatus
							.newBuilder()
							.mergeFrom(ByteString.copyFrom(response))
							.build();

					final int value = requestStatus.getValue();
					final boolean done = value < 0 || value >= 100;
					final boolean error = value < 0;

					if (done) {
						if (error) {
							final String errorMsg = "Flashing node " + reservedNode +
									" failed. Reason: " + requestStatus.getMsg();
							future.setException(new Exception(errorMsg));
						} else {
							future.set(true);
						}
					}

					return done;

				} catch (InvalidProtocolBufferException e) {
					log.error("Exception while parsing incoming request status: " + e, e);
					return false;
				}

			}

			@Override
			public void timeout() {
				future.setException(new TimeoutException());
			}

			@Override
			public void failure(Exception exception) {
				future.setException(exception);
			}
		};

		final ListenableFuture<Void> requestFuture =
				testbedRuntime.getSingleRequestMultiResponseService().sendUnreliableRequestUnreliableResponse(
						msg,
						2, TimeUnit.MINUTES,
						callback
				);

		requestFuture.addListener(new Runnable() {
			@Override
			public void run() {
				try {
					requestFuture.get();
				} catch (Exception e) {
					future.setException(e);
				}
			}
		}, MoreExecutors.sameThreadExecutor()
		);

		return future;
	}

	private void setDefaultPipelineOnReservedNodes() {

		log.info("Setting ChannelPipeline to default configuration for all nodes...");

		WSNAppMessages.Invocation.Builder operationInvocation = WSNAppMessages.Invocation
				.newBuilder()
				.setType(WSNAppMessages.Invocation.Type.SET_DEFAULT_CHANNEL_PIPELINE);

		final byte[] bytes = operationInvocation.build().toByteArray();
		final List<Future<byte[]>> futures = newArrayList();

		// fork
		for (String nodeUrn : reservedNodes) {

			futures.add(testbedRuntime.getReliableMessagingService().sendAsync(
					getLocalNodeName(),
					nodeUrn,
					MSG_TYPE_OPERATION_INVOCATION_REQUEST,
					bytes,
					UnreliableMessagingService.PRIORITY_NORMAL,
					10, TimeUnit.SECONDS
			)
			);
		}

		// join
		for (Future<byte[]> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				log.error("Exception while setting default pipeline on reserved nodes: {}", e.getMessage());
			}
		}
	}
}
