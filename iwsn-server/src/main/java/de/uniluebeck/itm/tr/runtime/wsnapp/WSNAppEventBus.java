package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSNAppEventBus extends EventBus {

	private static final Logger log = LoggerFactory.getLogger(WSNAppEventBus.class);

	public WSNAppEventBus() {
		super("WSNApp-EventBus");
	}

	@Override
	public void post(final Object event) {

		boolean isDownstreamMessage = event instanceof WSNAppDownstreamMessage;
		boolean isUpstreamMessage = event instanceof WSNAppUpstreamMessage;
		boolean isBackendNotifications = event instanceof WSNAppBackendNotifications;
		boolean isDeadEvent = event instanceof DeadEvent;

		if (!isDownstreamMessage && !isUpstreamMessage && !isBackendNotifications && !isDeadEvent) {
			throw new IllegalArgumentException("Events of type "
					+ event.getClass().getSimpleName()
					+ " are not allowed to be posted. Valid event types are: "
					+ WSNAppDownstreamMessage.class.getSimpleName() + ", "
					+ WSNAppUpstreamMessage.class.getSimpleName() + ", "
					+ WSNAppBackendNotifications.class.getSimpleName() + "."
			);
		}

		if (isDeadEvent) {
			log.warn("Dead event: {}", ((DeadEvent) event).getEvent());
		}

		super.post(event);
	}

	@Override
	public void register(final Object object) {
		log.debug("WSNAppEventBus.register({})", object);
		super.register(object);
	}

	@Override
	public void unregister(final Object object) {
		log.debug("WSNAppEventBus.unregister({})", object);
		super.unregister(object);
	}
}
