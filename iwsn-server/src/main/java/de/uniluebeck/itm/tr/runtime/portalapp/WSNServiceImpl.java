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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.wiseml.WiseMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convert;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convertCHCs;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convertNodeUrns;
import static de.uniluebeck.itm.tr.util.NetworkUtils.checkConnectivity;

public class WSNServiceImpl extends AbstractService implements WSNService {

	private static class RequestResultRunnable implements Runnable {

		private final List<String> nodeIds;

		private final Request request;

		private final DeliveryManager deliveryManager;

		private final long start;

		private RequestResultRunnable(final List<String> nodeIds, final Request request,
									  final DeliveryManager deliveryManager) {
			this.nodeIds = nodeIds;
			this.request = request;
			this.deliveryManager = deliveryManager;
			this.start = System.currentTimeMillis();
		}

		@Override
		public void run() {

			try {

				final RequestResult result = request.getFuture().get();

				if (log.isDebugEnabled()) {

					final long duration = System.currentTimeMillis() - start;

					for (Map.Entry<NodeUrn, Tuple<Integer, String>> entry : result.getResult().entrySet()) {

						final String nodeUrn = entry.getKey().toString();
						final Integer value = entry.getValue().getFirst();
						final String msg = entry.getValue().getSecond();

						log.debug(
								"{} => Received status for {} after {} ms: {}{}",
								new Object[]{
										nodeUrn,
										request.getClass().getSimpleName(),
										duration,
										value,
										msg == null ? "" : " (" + msg + ")"
								}
						);
					}
				}

				deliveryManager.receiveStatus(convert(result));

			} catch (InterruptedException e) {

				deliveryManager.receiveFailureStatusMessages(nodeIds, Long.toString(request.getRequestId()), e, -1);

			} catch (ExecutionException e) {

				if (e.getCause() instanceof UnknownNodeUrnsException) {

					UnknownNodeUrnsException exception = (UnknownNodeUrnsException) e.getCause();
					deliveryManager.receiveUnknownNodeUrnRequestStatus(
							exception.getNodeUrns(),
							e.getMessage(),
							Long.toString(request.getRequestId())
					);

				} else {

					deliveryManager.receiveFailureStatusMessages(nodeIds, Long.toString(request.getRequestId()), e, -1);
				}
			}
		}

		private static List<RequestStatus> convert(final RequestResult result) {

			List<RequestStatus> list = newArrayList();

			for (Map.Entry<NodeUrn, Tuple<Integer, String>> entry : result.getResult().entrySet()) {

				final RequestStatus requestStatus = new RequestStatus();
				requestStatus.setRequestId(Long.toString(result.getRequestId()));

				final NodeUrn nodeUrn = entry.getKey();
				final Integer value = entry.getValue().getFirst();
				final String msg = entry.getValue().getSecond();

				final Status status = new Status();
				status.setNodeId(nodeUrn.toString());
				status.setValue(value);
				status.setMsg(msg);

				requestStatus.getStatus().add(status);

				list.add(requestStatus);
			}

			return list;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(WSNService.class);

	private static DatatypeFactory DATATYPE_FACTORY;

	static {
		try {
			DATATYPE_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw propagate(e);
		}
	}

	private final WSNPreconditions preconditions;

	private final WSNServiceConfig config;

	private final Overlay overlay;

	private final RequestFactory requestFactory;

	private final DeliveryManager deliveryManager;

	private final WSNServiceVirtualLinkManager virtualLinkManager;

	@Inject
	public WSNServiceImpl(final Overlay overlay,
						  final RequestFactory requestFactory,
						  final DeliveryManager deliveryManager,
						  final WSNServiceVirtualLinkManager virtualLinkManager,
						  @Assisted final WSNServiceConfig config,
						  @Assisted final WSNPreconditions preconditions) {

		this.overlay = checkNotNull(overlay);
		this.requestFactory = checkNotNull(requestFactory);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.config = checkNotNull(config);
		this.preconditions = checkNotNull(preconditions);
		this.virtualLinkManager = checkNotNull(virtualLinkManager);
	}


	@Override
	protected void doStart() {

		try {

			log.info("Starting WSN service...");
			overlay.getEventBus().register(this);
			deliveryManager.startAndWait();
			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			log.info("Stopping WSN service...");

			overlay.getEventBus().unregister(this);

			deliveryManager.experimentEnded();
			deliveryManager.stopAndWait();

			log.info("Stopped WSN service!");

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Subscribe
	@VisibleForTesting
	void onMessageUpstreamRequest(final MessageUpstreamRequest request) {

		Message message = new Message();
		message.setSourceNodeId(request.getFrom().toString());
		message.setTimestamp(DATATYPE_FACTORY.newXMLGregorianCalendar(request.getTimestamp()));
		message.setBinaryData(request.getMessageBytes());

		deliveryManager.receive(message);
	}

	@Subscribe
	@VisibleForTesting
	void onRequestStatus(final de.uniluebeck.itm.tr.iwsn.newoverlay.RequestStatus requestStatus) {

		final RequestStatus soapStatus = new RequestStatus();
		soapStatus.setRequestId(Long.toString(requestStatus.getRequestId()));

		for (Map.Entry<NodeUrn, Tuple<Integer, String>> entry : requestStatus.getStatus().entrySet()) {

			final Status status = new Status();
			status.setNodeId(entry.getKey().toString());
			status.setValue(entry.getValue().getFirst());
			status.setMsg(entry.getValue().getSecond());

			soapStatus.getStatus().add(status);
		}

		deliveryManager.receiveStatus(soapStatus);
	}

	@Override
	public String getVersion() {
		return "2.3";
	}

	@Override
	public void addController(final String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.addController({})", controllerEndpointUrl);

		if (!"NONE".equals(controllerEndpointUrl)) {
			checkConnectivity(controllerEndpointUrl);
		}

		deliveryManager.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(String controllerEndpointUrl) {

		log.debug("WSNServiceImpl.removeController({})", controllerEndpointUrl);

		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	public String send(final List<String> nodeIds, final Message message) {

		log.debug("WSNServiceImpl.send({},{})", nodeIds, message);
		preconditions.checkSendArguments(nodeIds, message);

		final MessageDownstreamRequest request = requestFactory.createMessageDownstreamRequest(
				convertNodeUrns(nodeIds),
				message.getBinaryData()
		);
		return addResponseListenerAndPostRequest(nodeIds, request);
	}

	@Override
	public String setChannelPipeline(final List<String> nodes,
									 final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		log.debug("WSNServiceImpl.setChannelPipeline({}, {})", nodes, channelHandlerConfigurations);
		preconditions.checkSetChannelPipelineArguments(nodes, channelHandlerConfigurations);

		final SetChannelPipelineRequest request = requestFactory.createSetChannelPipelineRequest(
				convertNodeUrns(nodes),
				convertCHCs(channelHandlerConfigurations)
		);
		return addResponseListenerAndPostRequest(nodes, request);
	}

	@Override
	public String areNodesAlive(final List<String> nodeIds) {

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodeIds);
		preconditions.checkAreNodesAliveArguments(nodeIds);

		final AreNodesAliveRequest request = requestFactory.createAreNodesAliveRequest(convertNodeUrns(nodeIds));
		return addResponseListenerAndPostRequest(nodeIds, request);
	}

	@Override
	public String flashPrograms(final List<String> nodeIds,
								final List<Integer> programIndices,
								final List<Program> programs) {

		log.debug("WSNServiceImpl.flashPrograms(...)");

		preconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);

		// TODO implement

		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.flashPrograms(TypeConverter.convert(nodeIds, programIndices, programs), new WSNApp.Callback() {
				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

					if (log.isDebugEnabled()) {

						boolean hasInformation = requestStatus.hasStatus() &&
								requestStatus.getStatus().hasValue() &&
								requestStatus.getStatus().hasNodeId();

						if (hasInformation && requestStatus.getStatus().getValue() >= 0) {
							log.debug(
									"Flashing node {} completed {} percent.",
									requestStatus.getStatus().getNodeId(),
									requestStatus.getStatus().getValue()
							);
						} else if (hasInformation && requestStatus.getStatus().getValue() < 0) {
							log.warn(
									"Failed flashing node {} ({})!",
									requestStatus.getStatus().getNodeId(),
									requestStatus.getStatus().getValue()
							);
						}
					}

					// deliver output to client
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
		}

		return requestId;
	}

	@Override
	public List<ChannelHandlerDescription> getSupportedChannelHandlers() {

		HandlerFactoryRegistry handlerFactoryRegistry = new HandlerFactoryRegistry();
		try {
			ProtocolCollection.registerProtocols(handlerFactoryRegistry);
		} catch (Exception e) {
			throw propagate(e);
		}

		return convert(handlerFactoryRegistry.getChannelHandlerDescriptions());
	}

	@Override
	public String getNetwork() {
		log.debug("WSNServiceImpl.getNetwork");
		return WiseMLHelper.serialize(config.getWiseML());
	}

	@Override
	public String resetNodes(final List<String> nodeUrns) {

		log.debug("WSNServiceImpl.resetNodes({})", nodeUrns);
		preconditions.checkResetNodesArguments(nodeUrns);

		final ImmutableSet<NodeUrn> nodeUrnSet = convertNodeUrns(nodeUrns);
		return addResponseListenerAndPostRequest(nodeUrns, requestFactory.createResetNodesRequest(nodeUrnSet));
	}

	@Override
	public String setVirtualLink(final String sourceNode,
								 final String targetNode,
								 final String remoteServiceInstance,
								 final List<String> parameters,
								 final List<String> filters) {

		log.debug(
				"WSNServiceImpl.setVirtualLink({}, {}, {}, {}, {})",
				new Object[]{sourceNode, targetNode, remoteServiceInstance, parameters, filters}
		);
		preconditions.checkSetVirtualLinkArguments(sourceNode, targetNode, remoteServiceInstance, parameters, filters);

		final NodeUrn sourceNodeUrn = new NodeUrn(sourceNode);
		final NodeUrn targetNodeUrn = new NodeUrn(targetNode);

		final SetVirtualLinkRequest request = requestFactory.createSetVirtualLinkRequest(sourceNodeUrn, targetNodeUrn);
		final String requestId = addResponseListenerAndPostRequest(newArrayList(sourceNode), request);

		request.getFuture().addListener(new Runnable() {
			@Override
			public void run() {
				try {

					final RequestResult result = request.getFuture().get();
					if (result.getResult().get(sourceNodeUrn).getFirst() == 1) {
						virtualLinkManager.addVirtualLink(sourceNode, targetNode, remoteServiceInstance);
					}

				} catch (Exception e) {
					// handled by other listener
				}
			}
		}, MoreExecutors.sameThreadExecutor()
		);

		// TODO support filters

		return requestId;
	}

	@Override
	public String destroyVirtualLink(final String sourceNode,
									 final String targetNode) {

		log.debug("WSNServiceImpl.destroyVirtualLink({}, {})", sourceNode, targetNode);
		preconditions.checkDestroyVirtualLinkArguments(sourceNode, targetNode);

		final NodeUrn sourceNodeUrn = new NodeUrn(sourceNode);
		final NodeUrn targetNodeUrn = new NodeUrn(targetNode);

		final DestroyVirtualLinkRequest request = requestFactory.createDestroyVirtualLinkRequest(
				sourceNodeUrn,
				targetNodeUrn
		);

		final String requestId = addResponseListenerAndPostRequest(newArrayList(sourceNode), request);

		request.getFuture().addListener(new Runnable() {
			@Override
			public void run() {
				try {

					final RequestResult result = request.getFuture().get();
					if (result.getResult().get(sourceNodeUrn).getFirst() == 1) {
						virtualLinkManager.removeVirtualLink(sourceNode, targetNode);
					}

				} catch (Exception e) {
					// handled by other listener
				}
			}
		}, MoreExecutors.sameThreadExecutor()
		);

		return requestId;
	}

	@Override
	public String disableNode(final String node) {

		log.debug("WSNServiceImpl.disableNode({})", node);
		preconditions.checkDisableNodeArguments(node);

		final DisableNodeRequest request = requestFactory.createDisableNodeRequest(new NodeUrn(node));
		return addResponseListenerAndPostRequest(newArrayList(node), request);
	}

	@Override
	public String disablePhysicalLink(final String nodeA, final String nodeB) {

		log.debug("WSNServiceImpl.disablePhysicalLink({}, {})", nodeA, nodeB);
		preconditions.checkDisablePhysicalLinkArguments(nodeA, nodeB);

		final NodeUrn from = new NodeUrn(nodeA);
		final NodeUrn to = new NodeUrn(nodeB);

		final DisablePhysicalLinkRequest request = requestFactory.createDisablePhysicalLinkRequest(from, to);
		return addResponseListenerAndPostRequest(newArrayList(nodeA), request);
	}

	@Override
	public String enableNode(final String node) {

		log.debug("WSNServiceImpl.enableNode({})", node);
		preconditions.checkEnableNodeArguments(node);

		final EnableNodeRequest request = requestFactory.createEnableNodeRequest(new NodeUrn(node));
		return addResponseListenerAndPostRequest(newArrayList(node), request);
	}

	@Override
	public String enablePhysicalLink(final String nodeA, final String nodeB) {

		log.debug("WSNServiceImpl.enablePhysicalLink({}, {})", nodeA, nodeB);
		preconditions.checkEnablePhysicalLinkArguments(nodeA, nodeB);

		final NodeUrn to = new NodeUrn(nodeB);
		final NodeUrn from = new NodeUrn(nodeA);

		final EnablePhysicalLinkRequest request = requestFactory.createEnablePhysicalLinkRequest(from, to);
		return addResponseListenerAndPostRequest(newArrayList(nodeA), request);
	}

	@Override
	public List<String> getFilters() {
		log.debug("WSNServiceImpl.getFilters");
		throw new java.lang.UnsupportedOperationException("Method is not yet implemented.");
	}

	private String addResponseListenerAndPostRequest(final List<String> nodes, final Request request) {

		request.getFuture().addListener(
				new RequestResultRunnable(nodes, request, deliveryManager),
				MoreExecutors.sameThreadExecutor()
		);

		overlay.getEventBus().post(request);

		return Long.toString(request.getRequestId());
	}
}
