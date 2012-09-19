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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.netty.handlerstack.protocolcollection.ProtocolCollection;
import de.uniluebeck.itm.tr.iwsn.AuthorizationRequired;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.util.ProgressSettableFuture;
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
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.*;

public class WSNServiceImpl extends AbstractService implements WSNService {

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

	private final TestbedEventBus testbedEventBus;

	private final RequestFactory requestFactory;

	private final DeliveryManager deliveryManager;

	private final WSNServiceVirtualLinkManager virtualLinkManager;

	private final RequestIdProvider requestIdProvider;

	private final WSNServiceConfig config;

	private final WSNPreconditions preconditions;

	@Inject
	WSNServiceImpl(final TestbedEventBus testbedEventBus,
				   final RequestFactory requestFactory,
				   final DeliveryManager deliveryManager,
				   final WSNServiceVirtualLinkManager virtualLinkManager,
				   final RequestIdProvider requestIdProvider,
				   @Assisted final WSNServiceConfig config,
				   @Assisted final WSNPreconditions preconditions) {

		this.testbedEventBus = checkNotNull(testbedEventBus);
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
			testbedEventBus.register(this);
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

			testbedEventBus.unregister(this);

			deliveryManager.experimentEnded();
			deliveryManager.stopAndWait();

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	@Subscribe
	@VisibleForTesting
	public void onMessageUpstreamRequest(final MessageUpstreamRequest request) {

		log.debug("WSNServiceImpl.onMessageUpstreamRequest({})", request);

		Message message = new Message();
		message.setSourceNodeId(request.getFrom().toString());
		message.setTimestamp(DATATYPE_FACTORY.newXMLGregorianCalendar(request.getTimestamp()));
		message.setBinaryData(request.getMessageBytes());

		deliveryManager.receive(message);
	}

	@Subscribe
	@VisibleForTesting
	public void onBackendNotificationsRequest(final BackendNotificationsRequest request) {
		final ImmutableSet<String> notifications = request.getNotifications();
		deliveryManager.receiveNotification(newArrayList(notifications));
	}

	@Override
	public String getVersion() {
		return "2.3";
	}

	@Override
	public void addController(final String controllerEndpointUrl) {
		log.debug("WSNServiceImpl.addController({})", controllerEndpointUrl);
		deliveryManager.addController(controllerEndpointUrl);
	}

	@Override
	public void removeController(String controllerEndpointUrl) {
		log.debug("WSNServiceImpl.removeController({})", controllerEndpointUrl);
		deliveryManager.removeController(controllerEndpointUrl);
	}

	@Override
	@AuthorizationRequired("WSN_SEND")
	public String send(final List<String> nodeIds, final Message message) {

		log.debug("WSNServiceImpl.send({},{})", nodeIds, message);
		preconditions.checkSendArguments(nodeIds, message);

		final MessageDownstreamRequest request = requestFactory.createMessageDownstreamRequest(
				convertNodeUrns(nodeIds),
				message.getBinaryData()
		);
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_SET_CHANNEL_PIPELINE")
	public String setChannelPipeline(final List<String> nodes,
									 final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		log.debug("WSNServiceImpl.setChannelPipeline({}, {})", nodes, channelHandlerConfigurations);
		preconditions.checkSetChannelPipelineArguments(nodes, channelHandlerConfigurations);

		final SetChannelPipelineRequest request = requestFactory.createSetChannelPipelineRequest(
				convertNodeUrns(nodes),
				convertCHCs(channelHandlerConfigurations)
		);
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_ARE_NODES_ALIVE")
	public String areNodesAlive(final List<String> nodeIds) {

		log.debug("WSNServiceImpl.checkAreNodesAlive({})", nodeIds);
		preconditions.checkAreNodesAliveArguments(nodeIds);

		final AreNodesAliveRequest request = requestFactory.createAreNodesAliveRequest(convertNodeUrns(nodeIds));
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_FLASH_PROGRAMS")
	public String flashPrograms(final List<String> nodeIds,
								final List<Integer> programIndices,
								final List<Program> programs) {

		log.debug("WSNServiceImpl.flashPrograms()");

		preconditions.checkFlashProgramsArguments(nodeIds, programIndices, programs);

		final String clientRequestId = Long.toString(requestIdProvider.get());

		final ImmutableMap<byte[], ImmutableSet<NodeUrn>> flashJobs = convert(
				nodeIds,
				programIndices,
				programs
		);

		for (Map.Entry<byte[], ImmutableSet<NodeUrn>> entry : flashJobs.entrySet()) {

			final ImmutableSet<NodeUrn> nodeUrns = entry.getValue();
			final byte[] image = entry.getKey();

			final FlashImageRequest request = requestFactory.createFlashImageRequest(nodeUrns, image);

			overlayToClientRequestIdCache.put(request.getRequestId(), clientRequestId);
			addResponseListenerAndPostRequest(request, clientRequestId, 100);
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
	@AuthorizationRequired("WSN_RESET_NODES")
	public String resetNodes(final List<String> nodeUrns) {

		log.debug("WSNServiceImpl.resetNodes({})", nodeUrns);
		preconditions.checkResetNodesArguments(nodeUrns);

		final ImmutableSet<NodeUrn> nodeUrnSet = convertNodeUrns(nodeUrns);
		final ResetNodesRequest request = requestFactory.createResetNodesRequest(nodeUrnSet);
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_SET_VIRTUAL_LINK")
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
		final String requestId = addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);

		request.getFuture().addListener(new Runnable() {
			@Override
			public void run() {

				try {

					request.getFuture().get().get(sourceNodeUrn).get();
					virtualLinkManager.addVirtualLink(sourceNode, targetNode, remoteServiceInstance);

				} catch (Exception e) {
					// nothing to do
				}
			}
		}, MoreExecutors.sameThreadExecutor()
		);

		return requestId;
	}

	@Override
	@AuthorizationRequired("WSN_DESTROY_VIRTUAL_LINK")
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

		final String requestId = addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);

		request.getFuture().addListener(new Runnable() {
			@Override
			public void run() {

				try {

					request.getFuture().get().get(sourceNodeUrn).get();
					virtualLinkManager.removeVirtualLink(sourceNode, targetNode);

				} catch (Exception e) {
					// nothing to do
				}
			}
		}, MoreExecutors.sameThreadExecutor()
		);

		return requestId;
	}

	@Override
	@AuthorizationRequired("WSN_DISABLE_NODE")
	public String disableNode(final String node) {

		log.debug("WSNServiceImpl.disableNode({})", node);
		preconditions.checkDisableNodeArguments(node);

		final DisableNodeRequest request = requestFactory.createDisableNodeRequest(new NodeUrn(node));
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_DISABLE_PHYSICAL_LINK")
	public String disablePhysicalLink(final String nodeA, final String nodeB) {

		log.debug("WSNServiceImpl.disablePhysicalLink({}, {})", nodeA, nodeB);
		preconditions.checkDisablePhysicalLinkArguments(nodeA, nodeB);

		final NodeUrn from = new NodeUrn(nodeA);
		final NodeUrn to = new NodeUrn(nodeB);

		final DisablePhysicalLinkRequest request = requestFactory.createDisablePhysicalLinkRequest(from, to);
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_ENABLE_NODE")
	public String enableNode(final String node) {

		log.debug("WSNServiceImpl.enableNode({})", node);
		preconditions.checkEnableNodeArguments(node);

		final EnableNodeRequest request = requestFactory.createEnableNodeRequest(new NodeUrn(node));
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	@AuthorizationRequired("WSN_ENABLE_PHYSICAL_LINK")
	public String enablePhysicalLink(final String nodeA, final String nodeB) {

		log.debug("WSNServiceImpl.enablePhysicalLink({}, {})", nodeA, nodeB);
		preconditions.checkEnablePhysicalLinkArguments(nodeA, nodeB);

		final NodeUrn to = new NodeUrn(nodeB);
		final NodeUrn from = new NodeUrn(nodeA);

		final EnablePhysicalLinkRequest request = requestFactory.createEnablePhysicalLinkRequest(from, to);
		return addResponseListenerAndPostRequest(request, Long.toString(request.getRequestId()), 1);
	}

	@Override
	public List<String> getFilters() {
		log.debug("WSNServiceImpl.getFilters");
		throw new java.lang.UnsupportedOperationException("Method is not yet implemented.");
	}

	private String addResponseListenerAndPostRequest(final Request request, final String requestId,
													 final int completionValue) {

		for (Map.Entry<NodeUrn, ProgressSettableFuture<Void>> entry : request.getFutureMap().entrySet()) {

			final NodeUrn nodeUrn = entry.getKey();
			final ProgressSettableFuture<Void> nodeFuture = entry.getValue();

			nodeFuture.addListener(new Runnable() {
				@Override
				public void run() {
					try {

						nodeFuture.get();

						RequestStatus requestStatus = new RequestStatus();
						requestStatus.setRequestId(requestId);

						Status status = new Status();
						status.setNodeId(nodeUrn.toString());
						status.setValue(completionValue);
						status.setMsg("");

						requestStatus.getStatus().add(status);

						deliveryManager.receiveStatus(requestStatus);

					} catch (Exception e) {

						RequestStatus requestStatus = new RequestStatus();
						requestStatus.setRequestId(requestId);

						Status status = new Status();
						status.setNodeId(nodeUrn.toString());
						status.setValue(-1);
						status.setMsg(e.getMessage());

						requestStatus.getStatus().add(status);

						deliveryManager.receiveStatus(requestStatus);
					}
				}
			}, MoreExecutors.sameThreadExecutor()
			);

			nodeFuture.addProgressListener(new Runnable() {
				@Override
				public void run() {

					if (!request.getFutureMap().get(nodeUrn).isDone()) {

						RequestStatus requestStatus = new RequestStatus();
						requestStatus.setRequestId(requestId);

						Status status = new Status();
						status.setNodeId(nodeUrn.toString());
						status.setValue((int) (nodeFuture.getProgress() * 100));

						requestStatus.getStatus().add(status);

						deliveryManager.receiveStatus(requestStatus);
					}
				}
			}, MoreExecutors.sameThreadExecutor()
			);
		}

		testbedEventBus.post(request);
		return requestId;
	}
}
