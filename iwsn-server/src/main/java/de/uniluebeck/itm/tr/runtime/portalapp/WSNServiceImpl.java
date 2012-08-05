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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
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
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.*;
import static de.uniluebeck.itm.tr.util.NetworkUtils.checkConnectivity;

public class WSNServiceImpl extends AbstractService implements WSNService {

	private static class RequestResultRunnable implements Runnable {

		private final List<String> nodeIds;

		private final String requestId;

		private final Request request;

		private final DeliveryManager deliveryManager;

		private final long start;

		private RequestResultRunnable(final List<String> nodeIds,
									  final String requestId,
									  final Request request,
									  final DeliveryManager deliveryManager) {
			this.nodeIds = nodeIds;
			this.requestId = requestId;
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

				deliveryManager.receiveStatus(convert(result, requestId));

			} catch (InterruptedException e) {

				deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);

			} catch (ExecutionException e) {

				if (e.getCause() instanceof UnknownNodeUrnsException) {

					UnknownNodeUrnsException exception = (UnknownNodeUrnsException) e.getCause();
					deliveryManager.receiveUnknownNodeUrnRequestStatus(
							exception.getNodeUrns(),
							e.getMessage(),
							requestId
					);

				} else {

					deliveryManager.receiveFailureStatusMessages(nodeIds, requestId, e, -1);
				}
			}
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

	private final Cache<Long, String> overlayToClientRequestIdCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();

	private final EventBus eventBus;

	private final RequestFactory requestFactory;

	private final DeliveryManager deliveryManager;

	private final WSNServiceVirtualLinkManager virtualLinkManager;

	private final Provider<Long> requestIdProvider;

	private final WSNServiceConfig config;

	private final WSNPreconditions preconditions;

	@Inject
	public WSNServiceImpl(final EventBus eventBus,
						  final RequestFactory requestFactory,
						  final DeliveryManager deliveryManager,
						  final WSNServiceVirtualLinkManager virtualLinkManager,
						  final Provider<Long> requestIdProvider,
						  @Assisted final WSNServiceConfig config,
						  @Assisted final WSNPreconditions preconditions) {

		this.eventBus = checkNotNull(eventBus);
		this.requestFactory = checkNotNull(requestFactory);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.virtualLinkManager = checkNotNull(virtualLinkManager);
		this.requestIdProvider = checkNotNull(requestIdProvider);

		this.config = checkNotNull(config);
		this.preconditions = checkNotNull(preconditions);
	}


	@Override
	protected void doStart() {

		try {

			log.info("Starting WSN service...");
			eventBus.register(this);
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

			eventBus.unregister(this);

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

		final String cachedClientRequestId = overlayToClientRequestIdCache.getIfPresent(requestStatus.getRequestId());
		final String clientRequestId = cachedClientRequestId != null ?
				cachedClientRequestId :
				Long.toString(requestStatus.getRequestId());

		soapStatus.setRequestId(clientRequestId);

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
		return addResponseListenerAndPostRequest(nodeIds, request, Long.toString(request.getRequestId()));
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
		return addResponseListenerAndPostRequest(nodes, request, Long.toString(request.getRequestId()));
	}

	@Override
	public String areNodesAlive(final List<String> nodeIds) {

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodeIds);
		preconditions.checkAreNodesAliveArguments(nodeIds);

		final AreNodesAliveRequest request = requestFactory.createAreNodesAliveRequest(convertNodeUrns(nodeIds));
		return addResponseListenerAndPostRequest(nodeIds, request, Long.toString(request.getRequestId()));
	}

	@Override
	public String flashPrograms(final List<String> nodeIds,
								final List<Integer> programIndices,
								final List<Program> programs) {

		log.debug("WSNServiceImpl.flashPrograms()");

		preconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);

		final String clientRequestId = Long.toString(requestIdProvider.get());

		final ImmutableSet<Tuple<ImmutableSet<NodeUrn>, byte[]>> flashJobs = convert(
				nodeIds,
				programIndices,
				programs
		);

		for (Tuple<ImmutableSet<NodeUrn>, byte[]> flashJob : flashJobs) {

			final ImmutableSet<NodeUrn> nodeUrns = flashJob.getFirst();
			final byte[] image = flashJob.getSecond();

			final FlashImageRequest request = requestFactory.createFlashImageRequest(nodeUrns, image);

			overlayToClientRequestIdCache.put(request.getRequestId(), clientRequestId);
			addResponseListenerAndPostRequest(convert(nodeUrns), request, clientRequestId);
		}

		return clientRequestId;
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
		final ResetNodesRequest request = requestFactory.createResetNodesRequest(nodeUrnSet);
		return addResponseListenerAndPostRequest(nodeUrns, request, Long.toString(request.getRequestId()));
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
		final String requestId = addResponseListenerAndPostRequest(
				newArrayList(sourceNode),
				request,
				Long.toString(request.getRequestId())
		);

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

		final String requestId = addResponseListenerAndPostRequest(
				newArrayList(sourceNode),
				request,
				Long.toString(request.getRequestId())
		);

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
		return addResponseListenerAndPostRequest(newArrayList(node), request, Long.toString(request.getRequestId()));
	}

	@Override
	public String disablePhysicalLink(final String nodeA, final String nodeB) {

		log.debug("WSNServiceImpl.disablePhysicalLink({}, {})", nodeA, nodeB);
		preconditions.checkDisablePhysicalLinkArguments(nodeA, nodeB);

		final NodeUrn from = new NodeUrn(nodeA);
		final NodeUrn to = new NodeUrn(nodeB);

		final DisablePhysicalLinkRequest request = requestFactory.createDisablePhysicalLinkRequest(from, to);
		return addResponseListenerAndPostRequest(newArrayList(nodeA), request, Long.toString(request.getRequestId()));
	}

	@Override
	public String enableNode(final String node) {

		log.debug("WSNServiceImpl.enableNode({})", node);
		preconditions.checkEnableNodeArguments(node);

		final EnableNodeRequest request = requestFactory.createEnableNodeRequest(new NodeUrn(node));
		return addResponseListenerAndPostRequest(newArrayList(node), request, Long.toString(request.getRequestId()));
	}

	@Override
	public String enablePhysicalLink(final String nodeA, final String nodeB) {

		log.debug("WSNServiceImpl.enablePhysicalLink({}, {})", nodeA, nodeB);
		preconditions.checkEnablePhysicalLinkArguments(nodeA, nodeB);

		final NodeUrn to = new NodeUrn(nodeB);
		final NodeUrn from = new NodeUrn(nodeA);

		final EnablePhysicalLinkRequest request = requestFactory.createEnablePhysicalLinkRequest(from, to);
		return addResponseListenerAndPostRequest(newArrayList(nodeA), request, Long.toString(request.getRequestId()));
	}

	@Override
	public List<String> getFilters() {
		log.debug("WSNServiceImpl.getFilters");
		throw new java.lang.UnsupportedOperationException("Method is not yet implemented.");
	}

	private String addResponseListenerAndPostRequest(final List<String> nodes,
													 final Request request,
													 final String requestId) {

		final RequestResultRunnable listener = new RequestResultRunnable(nodes, requestId, request, deliveryManager);
		request.getFuture().addListener(listener, MoreExecutors.sameThreadExecutor());
		eventBus.post(request);
		return requestId;
	}
}
