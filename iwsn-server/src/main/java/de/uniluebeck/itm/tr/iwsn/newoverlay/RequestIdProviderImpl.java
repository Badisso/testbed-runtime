package de.uniluebeck.itm.tr.iwsn.newoverlay;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RequestIdProviderImpl implements RequestIdProvider {

	private long lastRequestId = 0;

	private final Lock lastRequestIdLock = new ReentrantLock();

	@Override
	public Long get() {
		lastRequestIdLock.lock();
		try {
			lastRequestId = lastRequestId == Long.MAX_VALUE ? 0 : ++lastRequestId;
			return lastRequestId;
		} finally {
			lastRequestIdLock.unlock();
		}
	}
}
