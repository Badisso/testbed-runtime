package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayEventBus;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayModule;

public class WSNAppOverlayModule extends OverlayModule {

	private final WSNApp wsnApp;

	public WSNAppOverlayModule(final WSNApp wsnApp) {
		this.wsnApp = wsnApp;
	}

	@Override
	protected void configure() {

		super.configure();

		bind(Overlay.class).to(WSNAppOverlay.class);
		bind(OverlayEventBus.class).toInstance(new OverlayEventBus());
		bind(WSNApp.class).toInstance(wsnApp);
	}
}
