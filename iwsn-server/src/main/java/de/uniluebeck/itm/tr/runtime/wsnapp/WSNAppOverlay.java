package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convert;
import static de.uniluebeck.itm.tr.runtime.portalapp.TypeConverter.convertToStringSet;

class WSNAppOverlay extends AbstractService implements Overlay {

	private static class WSNAppCallback implements WSNApp.Callback {

		private final Set<NodeUrn> responsesPending;

		private final int successValue;

		private final int errorValue;

		private final EventBus eventBus;

		private final Request request;

		private WSNAppCallback(final EventBus eventBus,
							   final Request request,
							   final int successValue,
							   final int errorValueLowerBoundExclusive) {

			this.eventBus = eventBus;
			this.request = request;

			this.successValue = successValue;
			this.errorValue = errorValueLowerBoundExclusive;

			this.responsesPending = newHashSet(request.getNodeUrns());
		}

		@Override
		public synchronized void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {

			final int value = requestStatus.getStatus().getValue();

			if (value >= successValue || value < errorValue) {
				responsesPending.remove(new NodeUrn(requestStatus.getStatus().getNodeId()));
			}

			if (responsesPending.isEmpty()) {
				eventBus.post(TypeConverter.convertToRequestResult(requestStatus, request.getRequestId()));
			} else {
				eventBus.post(TypeConverter.convertToRequestStatus(requestStatus, request.getRequestId()));
			}
		}

		@Override
		public synchronized void failure(Exception e) {
			request.getFuture().setException(e);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(WSNAppOverlay.class);

	private final OverlayEventBus eventBus;

	private final WSNApp wsnApp;

	private final RequestFactory requestFactory;

	@Inject
	@VisibleForTesting
	WSNAppOverlay(final OverlayEventBus eventBus, final WSNApp wsnApp, final RequestFactory requestFactory) {
		this.eventBus = checkNotNull(eventBus);
		this.wsnApp = checkNotNull(wsnApp);
		this.requestFactory = checkNotNull(requestFactory);
	}

	@Override
	public OverlayEventBus getEventBus() {
		return eventBus;
	}

	@Override
	protected void doStart() {
		try {
			eventBus.register(this);
			wsnApp.getEventBus().register(this);
			notifyStarted();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {
		try {
			eventBus.unregister(this);
			wsnApp.getEventBus().unregister(this);
			notifyStopped();
		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Subscribe
	protected void onDeadEvent(DeadEvent deadEvent) {
		log.error("Dead event: {}", deadEvent);
	}

	@Subscribe
	@VisibleForTesting
	void onAreNodesAliveRequest(final AreNodesAliveRequest request) {

		log.debug("WSNAppOverlay.onAreNodesAliveRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.areNodesAlive(convertToStringSet(request.getNodeUrns()), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onAreNodesAliveSmRequest(final AreNodesAliveSmRequest request) {

		log.debug("WSNAppOverlay.onAreNodesAliveSmRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.areNodesAliveSm(convertToStringSet(request.getNodeUrns()), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onDestroyVirtualLinkRequest(final DestroyVirtualLinkRequest request) {

		log.debug("WSNAppOverlay.onDestroyVirtualLinkRequest({})", request);

		try {

			final String sourceNodeUrn = request.getFrom().toString();
			final String targetNodeUrn = request.getTo().toString();
			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);

			wsnApp.destroyVirtualLink(sourceNodeUrn, targetNodeUrn, callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onDevicesAttachedEventRequest(final DevicesAttachedEventRequest request) {

		log.debug("WSNAppOverlay.onDevicesAttachedEventRequest({})", request);

		// TODO implement
		throw new RuntimeException("Not yet implemented!");
	}

	@Subscribe
	@VisibleForTesting
	void onDevicesDetachedEventRequest(final DevicesDetachedEventRequest request) {

		log.debug("WSNAppOverlay.onDevicesDetachedEventRequest({})", request);

		// TODO implement
		throw new RuntimeException("Not yet implemented!");
	}

	@Subscribe
	@VisibleForTesting
	void onDisableNodeRequest(final DisableNodeRequest request) {

		log.debug("WSNAppOverlay.onDisableNodeRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.disableNode(request.getNodeUrn().toString(), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onDisablePhysicalLinkRequest(final DisablePhysicalLinkRequest request) {

		log.debug("WSNAppOverlay.onDisablePhysicalLinkRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.disablePhysicalLink(request.getFrom().toString(), request.getTo().toString(), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onEnableNodeRequest(final EnableNodeRequest request) {

		log.debug("WSNAppOverlay.onEnableNodeRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.enableNode(request.getNodeUrn().toString(), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onEnablePhysicalLinkRequest(final EnablePhysicalLinkRequest request) {

		log.debug("WSNAppOverlay.onEnablePhysicalLinkRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.enablePhysicalLink(request.getFrom().toString(), request.getTo().toString(), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onFlashDefaultImageRequest(final FlashDefaultImageRequest request) {

		log.debug("WSNAppOverlay.onFlashDefaultImageRequest({})", request);

		// TODO implement
		throw new RuntimeException("Not yet implemented!");
	}

	@Subscribe
	@VisibleForTesting
	void onFlashImageRequest(final FlashImageRequest request) {

		log.debug("WSNAppOverlay.onFlashImageRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.flashPrograms(convert(request.getNodeUrns(), request.getImage()), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onMessageDownstreamRequest(final MessageDownstreamRequest request) {

		log.debug("WSNAppOverlay.onMessageDownstreamRequest({})", request);

		final WSNAppDownstreamMessage wsnAppDownstreamMessage = TypeConverter.convert(request);
		wsnAppDownstreamMessage.getFuture().addListener(new Runnable() {
			@Override
			public void run() {
				try {
					request.getFuture().set(
							TypeConverter.convert(
									wsnAppDownstreamMessage.getFuture().get(),
									request.getRequestId()
							)
					);
				} catch (Exception e) {
					request.getFuture().setException(e);
				}
			}
		}, MoreExecutors.sameThreadExecutor()
		);

		wsnApp.getEventBus().post(wsnAppDownstreamMessage);
	}

	@Subscribe
	@VisibleForTesting
	void onMessageUpstreamRequest(final MessageUpstreamRequest request) {

		log.debug("WSNAppOverlay.onMessageUpstreamRequest({})", request);

		throw new RuntimeException("This method should never be invoked on the portal host!");
	}

	@Subscribe
	@VisibleForTesting
	void onWSNAppUpstreamMessageRequest(final WSNAppUpstreamMessage request) {

		log.debug("WSNAppOverlay.onWSNAppUpstreamMessageRequest({})", request);

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

		eventBus.post(overlayRequest);
	}

	@Subscribe
	@VisibleForTesting
	void onResetNodesRequest(final ResetNodesRequest request) {

		log.debug("WSNAppOverlay.onResetNodesRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.resetNodes(convertToStringSet(request.getNodeUrns()), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onSetChannelPipelineRequest(final SetChannelPipelineRequest request) {

		log.debug("WSNAppOverlay.onSetChannelPipelineRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.resetNodes(convertToStringSet(request.getNodeUrns()), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}

	@Subscribe
	@VisibleForTesting
	void onSetDefaultChannelPipelineRequest(final SetDefaultChannelPipelineRequest request) {

		log.debug("WSNAppOverlay.onSetDefaultChannelPipelineRequest({})", request);

		// TODO implement
		throw new RuntimeException("Not yet implemented!");
	}

	@Subscribe
	@VisibleForTesting
	void onSetVirtualLinkRequest(final SetVirtualLinkRequest request) {

		log.debug("WSNAppOverlay.onSetVirtualLinkRequest({})", request);

		try {

			final WSNAppCallback callback = new WSNAppCallback(eventBus, request, 1, 0);
			wsnApp.setVirtualLink(request.getFrom().toString(), request.getTo().toString(), callback);

		} catch (UnknownNodeUrnsException e) {
			request.getFuture().setException(e);
		}
	}
}
