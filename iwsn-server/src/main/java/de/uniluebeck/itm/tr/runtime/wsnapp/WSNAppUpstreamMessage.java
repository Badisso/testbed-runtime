package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.SettableFuture;

public class WSNAppUpstreamMessage {

	private final String from;

	private final String timestamp;

	private final byte[] messageBytes;

	private final SettableFuture<Void> future;

	public WSNAppUpstreamMessage(final String from, final String timestamp, final byte[] messageBytes) {
		this.from = from;
		this.timestamp = timestamp;
		this.messageBytes = messageBytes;
		this.future = SettableFuture.create();
	}

	public String getFrom() {
		return from;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public SettableFuture<Void> getFuture() {
		return future;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("from", from)
				.add("timestamp", timestamp)
				.add("messageBytes", messageBytes.length + " bytes")
				.toString();
	}
}
