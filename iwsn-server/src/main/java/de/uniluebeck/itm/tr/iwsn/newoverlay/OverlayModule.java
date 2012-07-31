package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OverlayModule extends AbstractModule {

	private long lastRequestId = 0;

	private final Lock lastRequestIdLock = new ReentrantLock();

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().build(RequestFactory.class));
	}

	@Provides
	public long getNextRequestId() {
		lastRequestIdLock.lock();
		try {
			lastRequestId = lastRequestId == Long.MAX_VALUE ? 0 : ++lastRequestId;
			return lastRequestId;
		} finally {
			lastRequestIdLock.unlock();
		}
	}

}
