package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;

public class WSNAppBackendNotifications {

	private final ImmutableList<String> messages;

	private final SettableFuture<Void> future;

	public WSNAppBackendNotifications(final String... messages) {
		this(ImmutableList.copyOf(messages));
	}

	public WSNAppBackendNotifications(final ImmutableList<String> messages) {
		this.messages = messages;
		this.future = SettableFuture.create();
	}

	public ImmutableList<String> getMessages() {
		return messages;
	}

	public SettableFuture<Void> getFuture() {
		return future;
	}
}
