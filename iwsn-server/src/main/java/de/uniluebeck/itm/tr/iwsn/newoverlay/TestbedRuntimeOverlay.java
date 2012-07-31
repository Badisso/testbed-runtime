package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

import java.util.concurrent.TimeUnit;

public class TestbedRuntimeOverlay extends AbstractService implements Overlay {

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
	public ListenableFuture<Response> send(final Request request, final int timeout, final TimeUnit timeUnit) {
		return null;  // TODO implement
	}

	@Override
	public ListenableFuture<ProgressResponse> send(final ProgressRequest progressRequest, final int timeout,
												   final TimeUnit timeUnit) {
		return null;  // TODO implement
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
	protected void onDownstreamMessageEvent(DownstreamMessageEvent event) {
		// TODO implement
	}

	@Subscribe
	protected void onUpstreamMessageEvent(UpstreamMessageEvent event) {
		// TODO implement
	}

	@Subscribe
	protected void onDevicesAttachedEvent(DevicesAttachedEvent event) {
		// TODO implement
	}

	@Subscribe
	protected void onDevicesDetachedEvent(DevicesDetachedEvent event) {
		// TODO implement
	}
}
