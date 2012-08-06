package de.uniluebeck.itm.tr.runtime.wsnapp;

import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayModule;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

public class WSNAppOverlayModule extends OverlayModule {

	@Override
	protected void configure() {
		super.configure();
		bind(Overlay.class).to(WSNAppOverlay.class);
	}
}
