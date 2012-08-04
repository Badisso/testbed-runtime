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
		// TODO implement
		eventBus.register(this);
	}

	@Override
	protected void doStop() {
		eventBus.unregister(this);
		// TODO implement
	}

	@Subscribe
	protected void onDownstreamMessageEvent(MessageDownstreamRequest request) {
		// TODO implement
	}

	@Subscribe
	protected void onUpstreamMessageEvent(MessageUpstreamRequest request) {
		// TODO implement
	}

	@Subscribe
	protected void onDevicesAttachedEvent(DevicesAttachedEventRequest requestSendEvent) {
		// TODO implement
	}

	@Subscribe
	protected void onDevicesDetachedEvent(DevicesDetachedEventRequest eventRequest) {
		// TODO implement
	}

	@Subscribe
	protected void onDeadEvent(DeadEvent deadEvent) {
		log.error("Dead event: {}", deadEvent);
	}
}
