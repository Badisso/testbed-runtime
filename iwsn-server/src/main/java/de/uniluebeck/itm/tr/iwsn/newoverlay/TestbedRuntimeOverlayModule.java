package de.uniluebeck.itm.tr.iwsn.newoverlay;

import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

public class TestbedRuntimeOverlayModule extends OverlayModule {

	private final TestbedRuntime testbedRuntime;

	public TestbedRuntimeOverlayModule(final TestbedRuntime testbedRuntime) {
		this.testbedRuntime = testbedRuntime;
	}

	@Override
	protected void configure() {
		super.configure();
		bind(TestbedRuntime.class).toInstance(testbedRuntime);
	}
}
