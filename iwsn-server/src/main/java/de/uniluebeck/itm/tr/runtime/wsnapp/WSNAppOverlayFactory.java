package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayFactory;

public class WSNAppOverlayFactory implements OverlayFactory {

	private final WSNApp wsnApp;

	@Inject
	public WSNAppOverlayFactory(final WSNApp wsnApp) {
		this.wsnApp = wsnApp;
	}

	@Override
	public Overlay create(final EventBus eventBus) {
		return new WSNAppOverlay(eventBus, wsnApp);
	}
}
