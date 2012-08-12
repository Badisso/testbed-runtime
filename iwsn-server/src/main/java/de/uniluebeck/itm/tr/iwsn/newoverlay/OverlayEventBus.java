package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayEventBus extends EventBus {

	private static final Logger log = LoggerFactory.getLogger(OverlayEventBus.class);

	public OverlayEventBus() {
		super("Overlay-EventBus");
	}

	@Override
	public void post(final Object event) {
		log.trace("OverlayEventBus.post({})", event);
		super.post(event);
	}

	@Override
	public void register(final Object object) {
		log.debug("OverlayEventBus.register({})", object);
		super.register(object);
	}

	@Override
	public void unregister(final Object object) {
		log.debug("OverlayEventBus.unregister({})", object);
		super.unregister(object);
	}
}
