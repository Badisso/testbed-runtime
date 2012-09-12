package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter;
import de.uniluebeck.itm.tr.util.ProgressSettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convert;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convertToStringSet;

class WSNAppTestbed extends AbstractService implements Testbed {

	private TestbedEventBusSubscriber testbedEventBusSubscriber = new TestbedEventBusSubscriber();

	private WSNAppTestbed.WSNAppEventBusSubscriber wsnAppEventBusSubscriber = new WSNAppEventBusSubscriber();

	private static class WSNAppCallback implements WSNApp.Callback {

		private final int successValue;

		private final int errorValueLowerBoundExclusive;

		private final Request request;

		private WSNAppCallback(final Request request,
							   final int successValue,
							   final int errorValueLowerBoundExclusive) {

			this.request = request;
			this.successValue = successValue;
			this.errorValueLowerBoundExclusive = errorValueLowerBoundExclusive;
		}

		@Override
		public synchronized void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

			final NodeUrn nodeUrn = new NodeUrn(requestStatus.getNodeUrn());
			final ProgressSettableFuture<Void> nodeFuture = request.getFutureMap().get(nodeUrn);
			final int value = requestStatus.getValue();

			if (value >= successValue) {
				nodeFuture.set(null);
			} else if (value < errorValueLowerBoundExclusive) {
				nodeFuture.setException(new Exception(requestStatus.getMsg()));
			} else {
				nodeFuture.setProgress(((float) value / (float) 100));
			}
		}

		@Override
		public synchronized void failure(Exception e) {
			for (ProgressSettableFuture<Void> future : request.getFutureMap().values()) {
				future.setException(e);
			}
		}
	}

	private class TestbedEventBusSubscriber {

		private final Logger log = LoggerFactory.getLogger(TestbedEventBusSubscriber.class);

		@Subscribe
		@VisibleForTesting
		public void onAreNodesAliveRequest(final AreNodesAliveRequest request) {

			log.debug("WSNAppTestbed.onAreNodesAliveRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.areNodesAlive(convertToStringSet(request.getNodeUrns()), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		private void handleUnknownNodeUrnsException(final Request request, final UnknownNodeUrnsException e) {
			for (Map.Entry<NodeUrn, ProgressSettableFuture<Void>> entry : request.getFutureMap().entrySet()) {

				if (e.getNodeUrns().contains(entry.getKey().toString())) {
					entry.getValue().setException(e);
				} else {
					entry.getValue().setException(new Exception("Cancelled operation"));
				}
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onAreNodesAliveSmRequest(final AreNodesAliveSmRequest request) {

			log.debug("WSNAppTestbed.onAreNodesAliveSmRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.areNodesAliveSm(convertToStringSet(request.getNodeUrns()), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onDestroyVirtualLinkRequest(final DestroyVirtualLinkRequest request) {

			log.debug("WSNAppTestbed.onDestroyVirtualLinkRequest({})", request);

			try {

				final String sourceNodeUrn = request.getFrom().toString();
				final String targetNodeUrn = request.getTo().toString();
				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);

				wsnApp.destroyVirtualLink(sourceNodeUrn, targetNodeUrn, callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onDevicesAttachedEventRequest(final DevicesAttachedEventRequest request) {

			log.debug("WSNAppTestbed.onDevicesAttachedEventRequest({})", request);

			// TODO implement
			throw new RuntimeException("Not yet implemented!");
		}

		@Subscribe
		@VisibleForTesting
		public void onDevicesDetachedEventRequest(final DevicesDetachedEventRequest request) {

			log.debug("WSNAppTestbed.onDevicesDetachedEventRequest({})", request);

			// TODO implement
			throw new RuntimeException("Not yet implemented!");
		}

		@Subscribe
		@VisibleForTesting
		public void onDisableNodeRequest(final DisableNodeRequest request) {

			log.debug("WSNAppTestbed.onDisableNodeRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.disableNode(request.getNodeUrn().toString(), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onDisablePhysicalLinkRequest(final DisablePhysicalLinkRequest request) {

			log.debug("WSNAppTestbed.onDisablePhysicalLinkRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.disablePhysicalLink(request.getFrom().toString(), request.getTo().toString(), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onEnableNodeRequest(final EnableNodeRequest request) {

			log.debug("WSNAppTestbed.onEnableNodeRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.enableNode(request.getNodeUrn().toString(), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onEnablePhysicalLinkRequest(final EnablePhysicalLinkRequest request) {

			log.debug("WSNAppTestbed.onEnablePhysicalLinkRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.enablePhysicalLink(request.getFrom().toString(), request.getTo().toString(), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onFlashDefaultImageRequest(final FlashDefaultImageRequest request) {

			log.debug("WSNAppTestbed.onFlashDefaultImageRequest({})", request);

			// TODO implement
			throw new RuntimeException("Not yet implemented!");
		}

		@Subscribe
		@VisibleForTesting
		public void onFlashImageRequest(final FlashImageRequest request) {

			log.debug("WSNAppTestbed.onFlashImageRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.flashPrograms(convert(request.getNodeUrns(), request.getImage()), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onMessageDownstreamRequest(final MessageDownstreamRequest request) {

			log.debug("WSNAppTestbed.onMessageDownstreamRequest({})", request);

			final WSNAppDownstreamMessage wsnAppMessage = TypeConverter.convert(request);

			for (final Map.Entry<String, SettableFuture<Void>> entry : wsnAppMessage.getFutureMap().entrySet()) {

				final SettableFuture<Void> wsnAppNodeFuture = entry.getValue();
				final NodeUrn nodeUrn = new NodeUrn(entry.getKey());
				final ProgressSettableFuture<Void> overlayNodeFuture = request.getFutureMap().get(nodeUrn);

				wsnAppNodeFuture.addListener(new Runnable() {
					@Override
					public void run() {
						try {
							overlayNodeFuture.set(wsnAppNodeFuture.get());
						} catch (Exception e) {
							overlayNodeFuture.setException(e);
						}
					}
				}, MoreExecutors.sameThreadExecutor()
				);
			}

			wsnApp.getEventBus().post(wsnAppMessage);
		}

		@Subscribe
		@VisibleForTesting
		public void onResetNodesRequest(final ResetNodesRequest request) {

			log.debug("WSNAppTestbed.onResetNodesRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.resetNodes(convertToStringSet(request.getNodeUrns()), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onSetChannelPipelineRequest(final SetChannelPipelineRequest request) {

			log.debug("WSNAppTestbed.onSetChannelPipelineRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.resetNodes(convertToStringSet(request.getNodeUrns()), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}

		@Subscribe
		@VisibleForTesting
		public void onSetDefaultChannelPipelineRequest(final SetDefaultChannelPipelineRequest request) {

			log.debug("WSNAppTestbed.onSetDefaultChannelPipelineRequest({})", request);

			// TODO implement
			throw new RuntimeException("Not yet implemented!");
		}

		@Subscribe
		@VisibleForTesting
		public void onSetVirtualLinkRequest(final SetVirtualLinkRequest request) {

			log.debug("WSNAppTestbed.onSetVirtualLinkRequest({})", request);

			try {

				final WSNAppCallback callback = new WSNAppCallback(request, 1, 0);
				wsnApp.setVirtualLink(request.getFrom().toString(), request.getTo().toString(), callback);

			} catch (UnknownNodeUrnsException e) {
				handleUnknownNodeUrnsException(request, e);
			}
		}
	}

	private class WSNAppEventBusSubscriber {

		private final Logger log = LoggerFactory.getLogger(WSNAppEventBusSubscriber.class);

		@Subscribe
		@VisibleForTesting
		public void onWSNAppUpstreamMessageRequest(final WSNAppUpstreamMessage request) {

			log.trace("WSNAppTestbed.onWSNAppUpstreamMessageRequest({})", request);

			final MessageUpstreamRequest overlayRequest = TypeConverter.convert(request, requestFactory);

			overlayRequest.getFuture().addListener(new Runnable() {
				@Override
				public void run() {
					try {

						overlayRequest.getFuture().get();
						request.getFuture().set(null);

					} catch (Exception e) {
						request.getFuture().setException(e);
					}
				}
			}, MoreExecutors.sameThreadExecutor()
			);

			testbedEventBus.post(overlayRequest);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(WSNAppTestbed.class);

	private final TestbedEventBus testbedEventBus;

	private final WSNAppEventBus wsnAppEventBus;

	private final RequestFactory requestFactory;

	private final WSNApp wsnApp;

	@Inject
	@VisibleForTesting
	WSNAppTestbed(final TestbedEventBus testbedEventBus,
				  final WSNApp wsnApp,
				  final WSNAppEventBus wsnAppEventBus,
				  final RequestFactory requestFactory) {

		this.testbedEventBus = checkNotNull(testbedEventBus);
		this.wsnApp = checkNotNull(wsnApp);
		this.wsnAppEventBus = checkNotNull(wsnAppEventBus);
		this.requestFactory = checkNotNull(requestFactory);
	}

	@Override
	public TestbedEventBus getEventBus() {
		return testbedEventBus;
	}

	@Override
	protected void doStart() {

		log.trace("Starting WSNAppTestbed...");

		try {

			testbedEventBus.register(testbedEventBusSubscriber);
			wsnAppEventBus.register(wsnAppEventBusSubscriber);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		log.trace("Stopping WSNAppTestbed...");

		try {

			testbedEventBus.unregister(testbedEventBusSubscriber);
			wsnAppEventBus.unregister(wsnAppEventBusSubscriber);

			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}
}
