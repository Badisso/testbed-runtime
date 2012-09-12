package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.iwsn.newoverlay.Testbed;
import de.uniluebeck.itm.tr.iwsn.newoverlay.TestbedEventBus;
import de.uniluebeck.itm.tr.iwsn.newoverlay.TestbedModule;

public class WSNAppTestbedModule extends TestbedModule {

	private final WSNApp wsnApp;

	public WSNAppTestbedModule(final WSNApp wsnApp) {
		this.wsnApp = wsnApp;
	}

	@Override
	protected void configure() {

		super.configure();

		bind(Testbed.class).to(WSNAppTestbed.class);
		bind(TestbedEventBus.class).toInstance(new TestbedEventBus());

		bind(WSNApp.class).toInstance(wsnApp);
		bind(WSNAppEventBus.class).toInstance(wsnApp.getEventBus());
	}
}
