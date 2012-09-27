/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.MessageTools;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.event.MessageEventAdapter;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.event.MessageEventListener;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.srmr.SingleRequestMultiResponseListener;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.tr.util.ProgressListenableFuture;
import de.uniluebeck.itm.tr.util.StringUtils;
import de.uniluebeck.itm.tr.util.Tuple;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;


class WSNDeviceAppImpl extends AbstractService implements WSNDeviceApp {

	private class ReplyingFutureCallback implements FutureCallback<WSNDeviceAppConnector.Response> {

		private final Messages.Msg invocationMsg;

		private ReplyingFutureCallback(final Messages.Msg invocationMsg) {
			this.invocationMsg = invocationMsg;
		}

		@Override
		public void onSuccess(final WSNDeviceAppConnector.Response response) {
			sendResponse(response);
		}

		@Override
		public void onFailure(final Throwable throwable) {

			final WSNDeviceAppConnector.Response response;

			if (throwable instanceof TimeoutException) {
				response = new WSNDeviceAppConnector.Response(
						(byte) 0,
						"Communication to node timed out!".getBytes()
				);
			} else {
				response = new WSNDeviceAppConnector.Response(
						(byte) -1,
						("Exception: " + throwable.getMessage()).getBytes()
				);
			}

			sendResponse(response);
		}

		private void sendResponse(final WSNDeviceAppConnector.Response response) {

			final String message = response.payload == null ? null : new String(response.payload);
			final int code = (int) response.code;

			final Messages.Msg reply = MessageTools.buildReply(
					invocationMsg,
					WSNApp.MSG_TYPE_OPERATION_INVOCATION_RESPONSE,
					buildRequestStatus(code, message)
			);

			testbedRuntime.getUnreliableMessagingService().sendAsync(reply);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(WSNDeviceApp.class);

	/**
	 * A listener that is used to received messages from the overlay network.
	 */
	@Nonnull
	private final MessageEventListener messageEventListener = new MessageEventAdapter() {

		@Override
		public void messageReceived(Messages.Msg msg) {

			boolean isRecipient = wsnDeviceAppConfiguration.getNodeUrn().equals(msg.getTo());
			boolean isOperationInvocation = WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST.equals(msg.getMsgType());

			if (isRecipient && isOperationInvocation) {

				log.trace("{} => Received message of type {}...", wsnDeviceAppConfiguration.getNodeUrn(),
						msg.getMsgType()
				);

				WSNAppMessages.Invocation invocation = parseOperation(msg);

				if (invocation != null) {
					log.trace("{} => Operation parsed: {}", wsnDeviceAppConfiguration.getNodeUrn(),
							invocation.getType()
					);
					executeInvocation(invocation, msg);
				}

			}
		}
	};

	@Nonnull
	private final SingleRequestMultiResponseListener srmrsListener = new SingleRequestMultiResponseListener() {
		@Override
		public void receiveRequest(Messages.Msg msg, Responder responder) {

			try {

				final WSNAppMessages.Invocation invocation =
						WSNAppMessages.Invocation.newBuilder().mergeFrom(msg.getPayload()).build();

				switch (invocation.getType()) {
					case FLASH_PROGRAMS:
						executeFlashPrograms(invocation.getFlashImageRequest().getImage().toByteArray(), responder);
						break;
					case FLASH_DEFAULT_IMAGE:
						executeFlashDefaultImage(responder);
						break;
				}

			} catch (InvalidProtocolBufferException e) {
				log.error("{} => Error while parsing operation invocation. Ignoring...: {}",
						wsnDeviceAppConfiguration.getNodeUrn(), e
				);
			}
		}
	};

	@Nonnull
	private final WSNDeviceAppConnector.NodeOutputListener nodeOutputListener =
			new WSNDeviceAppConnector.NodeOutputListener() {

				@Override
				public void receivedPacket(final byte[] bytes) {

					final XMLGregorianCalendar now = datatypeFactory
							.newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance());

					final WSNAppMessages.UpstreamMessage.Builder messageBuilder =
							WSNAppMessages.UpstreamMessage.newBuilder()
									.setSourceNodeUrn(wsnDeviceAppConfiguration.getNodeUrn())
									.setTimestamp(now.toXMLFormat())
									.setMessageBytes(ByteString.copyFrom(bytes));

					final WSNAppMessages.UpstreamMessage message = messageBuilder.build();

					if (log.isDebugEnabled()) {
						log.debug("{} => Delivering device output to overlay node {}: {}", new String[]{
								wsnDeviceAppConfiguration.getNodeUrn(),
								wsnDeviceAppConfiguration.getPortalNodeUrn(),
								WSNAppMessageTools.toString(message, false, 200)
						}
						);
					}

					testbedRuntime.getUnreliableMessagingService().sendAsync(
							wsnDeviceAppConfiguration.getNodeUrn(),
							wsnDeviceAppConfiguration.getPortalNodeUrn(),
							WSNApp.MSG_TYPE_LISTENER_MESSAGE,
							message.toByteArray(),
							UnreliableMessagingService.PRIORITY_NORMAL
					);
				}

				@Override
				public void receiveNotification(final String notificationString) {

					final WSNAppMessages.Notification message = WSNAppMessages.Notification.newBuilder()
							.setMsg(notificationString)
							.setNodeUrn(wsnDeviceAppConfiguration.getNodeUrn())
							.build();


					if (log.isDebugEnabled()) {
						log.debug("{} => Delivering notification to {}: {}", new String[]{
								wsnDeviceAppConfiguration.getNodeUrn(),
								wsnDeviceAppConfiguration.getPortalNodeUrn(),
								notificationString
						}
						);
					}

					testbedRuntime.getUnreliableMessagingService().sendAsync(
							wsnDeviceAppConfiguration.getNodeUrn(),
							wsnDeviceAppConfiguration.getPortalNodeUrn(),
							WSNApp.MSG_TYPE_LISTENER_NOTIFICATION,
							message.toByteArray(),
							UnreliableMessagingService.PRIORITY_NORMAL
					);
				}
			};

	/**
	 * The connector to the actual sensor node. May be local (i.e. attached to a serial port) or remote (i.e. (multi-hop)
	 * through remote-UART.
	 */
	@Nonnull
	private WSNDeviceAppConnector connector;

	@Nonnull
	private final WSNDeviceAppConfiguration wsnDeviceAppConfiguration;

	@Nonnull
	private final WSNDeviceAppConnectorFactory wsnDeviceAppConnectorFactory;

	@Nonnull
	private final WSNDeviceAppConnectorConfiguration wsnDeviceAppConnectorConfiguration;

	/**
	 * A reference to the overlay network used to receive and send messages.
	 */
	@Nonnull
	private final TestbedRuntime testbedRuntime;

	@Nonnull
	private final DeviceFactory deviceFactory;

	@Nonnull
	private final DatatypeFactory datatypeFactory;

	@Inject
	public WSNDeviceAppImpl(@Assisted final TestbedRuntime testbedRuntime,
							@Assisted final DeviceFactory deviceFactory,
							@Assisted final WSNDeviceAppConfiguration wsnDeviceAppConfiguration,
							@Assisted final WSNDeviceAppConnectorConfiguration wsnDeviceAppConnectorConfiguration,
							final WSNDeviceAppConnectorFactory wsnDeviceAppConnectorFactory) {

		this.testbedRuntime = checkNotNull(testbedRuntime);
		this.deviceFactory = checkNotNull(deviceFactory);
		this.wsnDeviceAppConfiguration = checkNotNull(wsnDeviceAppConfiguration);
		this.wsnDeviceAppConnectorFactory = checkNotNull(wsnDeviceAppConnectorFactory);
		this.wsnDeviceAppConnectorConfiguration = checkNotNull(wsnDeviceAppConnectorConfiguration);

		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parses incoming operation invocation and dispatches it to the corresponding method in the WSNDeviceAppConnector
	 * instance.
	 *
	 * @param invocation
	 * 		the protobuf message that describes the operation to be invoked
	 * @param invocationMsg
	 * 		the protobuf message in which the operation invocation message was wrapped
	 */
	private void executeInvocation(WSNAppMessages.Invocation invocation, Messages.Msg invocationMsg) {

		final ReplyingFutureCallback callback = new ReplyingFutureCallback(invocationMsg);

		switch (invocation.getType()) {

			case ARE_NODES_ALIVE:
				executeAreNodesAlive(invocation.getAreNodesAliveRequest(), callback);
				break;

			case ARE_NODES_CONNECTED:
				executeAreNodesConnected(invocation.getAreNodesConnectedRequest(), callback);
				break;

			case DESTROY_VIRTUAL_LINK:
				executeDestroyVirtualLink(invocation.getDestroyVirtualLinksRequest(), callback);
				break;

			case DISABLE_NODE:
				executeDisableNode(invocation.getDisableNodesRequest(), callback);
				break;

			case DISABLE_PHYSICAL_LINK:
				executeDisablePhysicalLinks(invocation.getDisablePhysicalLinksRequest(), callback);
				break;

			case ENABLE_NODE:
				executeEnableNode(invocation.getEnableNodesRequest(), callback);
				break;

			case ENABLE_PHYSICAL_LINK:
				executeEnablePhysicalLink(invocation.getEnablePhysicalLinksRequest(), callback);
				break;

			case RESET_NODES:
				executeResetNode(invocation.getResetNodesRequest(), callback);
				break;

			case SEND_DOWNSTREAM:
				executeSendMessage(invocation.getDownstreamMessage(), callback);
				break;

			case SET_DEFAULT_CHANNEL_PIPELINE:
				executeSetDefaultChannelPipeline(invocation.getSetDefaultChannelPipelineRequest(), callback);
				break;

			case SET_CHANNEL_PIPELINE:
				executeSetChannelPipeline(invocation.getSetChannelPipelineRequest(), callback);
				break;

			case SET_VIRTUAL_LINK:
				executeSetVirtualLink(invocation.getSetVirtualLinksRequest(), callback);
				break;

			default:
				throw new RuntimeException("Unknown invocation type!");
		}
	}

	private void executeAreNodesAlive(final WSNAppMessages.AreNodesAliveRequest request,
									  final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeAreNodesAlive()");
		addCallback(connector.isNodeAlive(), callback);
	}

	private void executeAreNodesConnected(final WSNAppMessages.AreNodesConnectedRequest request,
										  final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeAreNodesConnected()");
		addCallback(connector.isNodeConnected(), callback);
	}

	private void executeDisableNode(final WSNAppMessages.DisableNodesRequest request,
									final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeDisableNode()");
		addCallback(connector.disableNode(), callback);
	}

	private void executeDisablePhysicalLinks(final WSNAppMessages.DisablePhysicalLinksRequest invocation,
											 final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeDisablePhysicalLinks()");
		final WSNAppMessages.Link link = invocation.getLinks(0);
		ListenableFuture<WSNDeviceAppConnector.Response> future;
		try {

			long nodeB = StringUtils.parseHexOrDecLongFromUrn(link.getTargetNodeUrn());
			future = connector.disablePhysicalLink(nodeB);

		} catch (NumberFormatException e) {
			final String errorMsg =
					"Suffix of destination node URN \"" + link.getTargetNodeUrn() + "\" is not a valid long value!";
			future = immediateFuture(new WSNDeviceAppConnector.Response((byte) -1, errorMsg.getBytes()));
		}

		addCallback(future, callback);
	}

	private void executeResetNode(final WSNAppMessages.ResetNodesRequest request,
								  final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeResetNode()");
		addCallback(connector.resetNode(), callback);
	}

	private void executeEnableNode(final WSNAppMessages.EnableNodesRequest request,
								   final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeEnableNode()");
		addCallback(connector.enableNode(), callback);
	}

	private void executeEnablePhysicalLink(final WSNAppMessages.EnablePhysicalLinksRequest request,
										   final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeEnablePhysicalLink()");

		final WSNAppMessages.Link link = request.getLinks(0);
		ListenableFuture<WSNDeviceAppConnector.Response> future;
		try {

			long nodeB = StringUtils.parseHexOrDecLongFromUrn(link.getTargetNodeUrn());
			future = connector.enablePhysicalLink(nodeB);

		} catch (NumberFormatException e) {
			final String errorMsg =
					"Suffix of destination node URN \"" + link.getTargetNodeUrn() + "\" is not a valid long value!";
			future = immediateFuture(new WSNDeviceAppConnector.Response((byte) -1, errorMsg.getBytes()));
		}

		addCallback(future, callback);
	}

	private void executeSetDefaultChannelPipeline(final WSNAppMessages.SetDefaultChannelPipelineRequest request,
												  final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeSetDefaultChannelPipeline()");
		addCallback(connector.setDefaultChannelPipeline(), callback);
	}

	private void executeSetChannelPipeline(final WSNAppMessages.SetChannelPipelineRequest request,
										   final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeSetChannelPipeline()");
		addCallback(connector.setChannelPipeline(convert(request)), callback);
	}

	public void executeDestroyVirtualLink(final WSNAppMessages.DestroyVirtualLinksRequest destroyVirtualLinkRequest,
										  final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeDestroyVirtualLink()");
		final WSNAppMessages.Link link = destroyVirtualLinkRequest.getLinks(0);
		ListenableFuture<WSNDeviceAppConnector.Response> future;
		try {

			long destinationNode = StringUtils.parseHexOrDecLongFromUrn(link.getTargetNodeUrn());
			future = connector.destroyVirtualLink(destinationNode);

		} catch (NumberFormatException e) {
			final String errorMsg =
					"Suffix of destination node URN \"" + link.getTargetNodeUrn() + "\" is not a valid long value!";
			future = immediateFuture(new WSNDeviceAppConnector.Response((byte) -1, errorMsg.getBytes()));
		}

		addCallback(future, callback);
	}

	public void executeSetVirtualLink(final WSNAppMessages.SetVirtualLinksRequest setVirtualLinkRequest,
									  final ReplyingFutureCallback callback) {

		log.debug("WSNDeviceAppImpl.executeSetVirtualLink()");
		final WSNAppMessages.Link link = setVirtualLinkRequest.getLinks(0);
		ListenableFuture<WSNDeviceAppConnector.Response> future;
		try {

			long destinationNode = StringUtils.parseHexOrDecLongFromUrn(link.getTargetNodeUrn());
			future = connector.setVirtualLink(destinationNode);

		} catch (Exception e) {
			log.warn("{} => Received destinationNode URN whose suffix could not be parsed to long: {}",
					wsnDeviceAppConfiguration.getNodeUrn(), link.getTargetNodeUrn()
			);
			future = immediateFuture(new WSNDeviceAppConnector.Response((byte) -1,
					"Destination node URN suffix is not a valid long value!".getBytes()
			)
			);
		}

		addCallback(future, callback);
	}

	public void executeSendMessage(final WSNAppMessages.DownstreamMessage message,
								   final ReplyingFutureCallback callback) {

		log.debug("{} => WSNDeviceAppImpl.executeSendMessage()", wsnDeviceAppConfiguration.getNodeUrn());
		addCallback(connector.sendMessage(message.getMessageBytes().toByteArray()), callback);
	}

	public void executeFlashDefaultImage(final SingleRequestMultiResponseListener.Responder responder) {

		log.debug("{} => WSNDeviceAppImpl.executeFlashDefaultImage()", wsnDeviceAppConfiguration.getNodeUrn());

		try {

			if (wsnDeviceAppConfiguration.getDefaultImageFile() != null) {
				final byte[] binaryImage = Files.toByteArray(wsnDeviceAppConfiguration.getDefaultImageFile());
				executeFlashPrograms(binaryImage, responder);
			} else {
				responder.sendResponse(buildRequestStatus(100, null));
			}

		} catch (IOException e) {
			log.error("{} => Exception while reading default image: {}", wsnDeviceAppConfiguration.getNodeUrn(), e);
			throw new RuntimeException(e);
		}

	}

	public void executeFlashPrograms(final byte[] binaryImage,
									 final SingleRequestMultiResponseListener.Responder responder) {

		log.debug("{} => WSNDeviceAppImpl.executeFlashPrograms()", wsnDeviceAppConfiguration.getNodeUrn());

		final ProgressListenableFuture<WSNDeviceAppConnector.Response> future = connector.flashProgram(binaryImage);

		future.addProgressListener(createFlashProgramProgressListener(responder, future), sameThreadExecutor());
		future.addListener(createFlashProgramListener(responder, future), sameThreadExecutor());
	}

	private Runnable createFlashProgramListener(final SingleRequestMultiResponseListener.Responder responder,
												final ProgressListenableFuture<WSNDeviceAppConnector.Response> future) {
		return new Runnable() {
			@Override
			public void run() {

				WSNDeviceAppConnector.Response response;
				try {
					response = future.get();
					log.debug("{} => WSNDeviceAppImpl.flashProgram.success()", wsnDeviceAppConfiguration.getNodeUrn());
				} catch (Exception e) {
					log.debug("{} => WSNDeviceAppImpl.flashProgram.failed()", wsnDeviceAppConfiguration.getNodeUrn());
					if (e instanceof TimeoutException) {
						response = new WSNDeviceAppConnector.Response(
								(byte) 0,
								"Communication to node timed out!".getBytes()
						);
					} else {
						response = new WSNDeviceAppConnector.Response(
								(byte) -1,
								("Exception: " + e.getMessage()).getBytes()
						);
					}
				}

				final String message = response.payload == null ? null : new String(response.payload);
				final int code = (int) response.code;

				responder.sendResponse(buildRequestStatus(code, message));
			}
		};
	}

	private Runnable createFlashProgramProgressListener(final SingleRequestMultiResponseListener.Responder responder,
														final ProgressListenableFuture<WSNDeviceAppConnector.Response> future) {
		return new Runnable() {
			@Override
			public void run() {
				responder.sendResponse(buildRequestStatus((int) (future.getProgress() * 100), null));
			}
		};
	}

	private WSNAppMessages.Invocation parseOperation(Messages.Msg msg) {
		try {
			return WSNAppMessages.Invocation.parseFrom(msg.getPayload());
		} catch (InvalidProtocolBufferException e) {
			log.warn("{} => Couldn't parse operation invocation message: {}. Ignoring...",
					wsnDeviceAppConfiguration.getNodeUrn(), e
			);
			return null;
		}
	}

	@Override
	public String getName() {
		return WSNDeviceApp.class.getSimpleName();
	}

	@Override
	protected void doStart() {

		try {

			log.debug("{} => WSNDeviceAppImpl.start()", wsnDeviceAppConfiguration.getNodeUrn());

			// connect to device
			connector = wsnDeviceAppConnectorFactory.create(
					wsnDeviceAppConnectorConfiguration,
					deviceFactory,
					testbedRuntime.getEventBus(),
					testbedRuntime.getAsyncEventBus()
			);

			connector.startAndWait();
			connector.addListener(nodeOutputListener);

			testbedRuntime.getMessageEventService().addListener(messageEventListener);

			// start listening to invocation messages
			testbedRuntime.getSingleRequestMultiResponseService().addListener(
					wsnDeviceAppConfiguration.getNodeUrn(),
					WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST,
					srmrsListener
			);

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStarted();
	}

	@Override
	protected void doStop() {

		try {

			log.debug("{} => WSNDeviceAppImpl.stop()", wsnDeviceAppConfiguration.getNodeUrn());

			// first stop listening to invocation messages
			testbedRuntime.getMessageEventService().removeListener(messageEventListener);
			testbedRuntime.getSingleRequestMultiResponseService().removeListener(srmrsListener);

			// then disconnect from device
			connector.removeListener(nodeOutputListener);
			connector.stopAndWait();

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}

	/**
	 * Helper method to build a RequestStatus object for asynchronous reply to an operation invocation.
	 *
	 * @param value
	 * 		the operations return code
	 * @param message
	 * 		a message to the invoker
	 *
	 * @return the serialized RequestStatus instance created
	 */
	private byte[] buildRequestStatus(int value, @Nullable String message) {

		WSNAppMessages.RequestStatus.Builder requestStatusBuilder = WSNAppMessages.RequestStatus
				.newBuilder()
				.setNodeUrn(wsnDeviceAppConfiguration.getNodeUrn())
				.setValue(value);

		if (message != null) {
			requestStatusBuilder.setMsg(message);
		}

		return requestStatusBuilder.build().toByteArray();

	}

	private List<Tuple<String, Multimap<String, String>>> convert(
			final WSNAppMessages.SetChannelPipelineRequest request) {

		final List<Tuple<String, Multimap<String, String>>> result = newArrayList();

		for (WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration channelHandlerConfiguration : request
				.getChannelHandlerConfigurationsList()) {

			result.add(convert(channelHandlerConfiguration));
		}

		return result;
	}

	private Tuple<String, Multimap<String, String>> convert(
			final WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration channelHandlerConfiguration) {

		return new Tuple<String, Multimap<String, String>>(
				channelHandlerConfiguration.getName(),
				convert(channelHandlerConfiguration.getConfigurationList())
		);
	}

	private Multimap<String, String> convert(
			final List<WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair> configurationList) {

		final HashMultimap<String, String> result = HashMultimap.create();
		for (WSNAppMessages.SetChannelPipelineRequest.ChannelHandlerConfiguration.KeyValuePair keyValuePair : configurationList) {
			result.put(keyValuePair.getKey(), keyValuePair.getValue());
		}
		return result;
	}

}
