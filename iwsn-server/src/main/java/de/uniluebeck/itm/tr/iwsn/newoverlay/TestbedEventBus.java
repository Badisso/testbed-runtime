package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestbedEventBus extends EventBus {

	private static final Logger log = LoggerFactory.getLogger(TestbedEventBus.class);

	public TestbedEventBus() {
		super("Testbed-EventBus");
	}

	@Override
	public void post(final Object event) {
		log.trace("TestbedEventBus.post({})", event);
		super.post(event);
	}

	@Override
	public void register(final Object object) {
		log.debug("TestbedEventBus.register({})", object);
		super.register(object);
	}

	@Override
	public void unregister(final Object object) {
		log.debug("TestbedEventBus.unregister({})", object);
		super.unregister(object);
	}
}
