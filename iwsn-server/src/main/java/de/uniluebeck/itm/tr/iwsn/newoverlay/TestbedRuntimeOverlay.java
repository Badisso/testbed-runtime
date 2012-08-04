package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestbedRuntimeOverlay extends AbstractService implements Overlay {

	private static final Logger log = LoggerFactory.getLogger(TestbedRuntimeOverlay.class);

	private final EventBus eventBus;

	private final TestbedRuntime testbedRuntime;

	@Inject
	@VisibleForTesting
	TestbedRuntimeOverlay(final EventBus eventBus, final TestbedRuntime testbedRuntime) {
		this.eventBus = eventBus;
		this.testbedRuntime = testbedRuntime;
	}

	@Override
	public EventBus getEventBus() {
		return eventBus;
	}

	@Override
	protected void doStart() {
		try {
			testbedRuntime.startAndWait();
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
			testbedRuntime.stopAndWait();
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
	void onAreNodesAliveRequest(AreNodesAliveRequest request) {
		log.debug("TestbedRuntimeOverlay.onAreNodesAliveRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onAreNodesAliveSmRequest(AreNodesAliveSmRequest request) {
		log.debug("TestbedRuntimeOverlay.onAreNodesAliveSmRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onDestroyVirtualLinkRequest(DestroyVirtualLinkRequest request) {
		log.debug("TestbedRuntimeOverlay.onDestroyVirtualLinkRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onDevicesAttachedEventRequest(DevicesAttachedEventRequest request) {
		log.debug("TestbedRuntimeOverlay.onDevicesAttachedEventRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onDevicesDetachedEventRequest(DevicesDetachedEventRequest request) {
		log.debug("TestbedRuntimeOverlay.onDevicesDetachedEventRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onDisableNodeRequest(DisableNodeRequest request) {
		log.debug("TestbedRuntimeOverlay.onDisableNodeRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onDisablePhysicalLinkRequest(DisablePhysicalLinkRequest request) {
		log.debug("TestbedRuntimeOverlay.onDisablePhysicalLinkRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onEnableNodeRequest(EnableNodeRequest request) {
		log.debug("TestbedRuntimeOverlay.onEnableNodeRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onEnablePhysicalLinkRequest(EnablePhysicalLinkRequest request) {
		log.debug("TestbedRuntimeOverlay.onEnablePhysicalLinkRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onFlashDefaultImageRequest(FlashDefaultImageRequest request) {
		log.debug("TestbedRuntimeOverlay.onFlashDefaultImageRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onFlashImageRequest(FlashImageRequest request) {
		log.debug("TestbedRuntimeOverlay.onFlashImageRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onMessageDownstreamRequest(MessageDownstreamRequest request) {
		log.debug("TestbedRuntimeOverlay.onMessageDownstreamRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onMessageUpstreamRequest(MessageUpstreamRequest request) {
		log.debug("TestbedRuntimeOverlay.onMessageUpstreamRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onResetNodesRequest(ResetNodesRequest request) {
		log.debug("TestbedRuntimeOverlay.onResetNodesRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onSetChannelPipelineRequest(SetChannelPipelineRequest request) {
		log.debug("TestbedRuntimeOverlay.onSetChannelPipelineRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onSetDefaultChannelPipelineRequest(SetDefaultChannelPipelineRequest request) {
		log.debug("TestbedRuntimeOverlay.onSetDefaultChannelPipelineRequest({})", request);
		// TODO implement
	}

	@Subscribe
	@VisibleForTesting
	void onSetVirtualLinkRequest(SetVirtualLinkRequest request) {
		log.debug("TestbedRuntimeOverlay.onSetVirtualLinkRequest({})", request);
		// TODO implement
	}
}
