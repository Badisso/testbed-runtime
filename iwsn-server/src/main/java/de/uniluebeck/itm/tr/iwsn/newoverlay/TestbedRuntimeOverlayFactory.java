package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

public class TestbedRuntimeOverlayFactory implements OverlayFactory {

	private final TestbedRuntime testbedRuntime;

	@Inject
	public TestbedRuntimeOverlayFactory(final TestbedRuntime testbedRuntime) {
		this.testbedRuntime = testbedRuntime;
	}

	@Override
	public Overlay create(final EventBus eventBus) {
		return new TestbedRuntimeOverlay(eventBus, testbedRuntime);
	}
}
