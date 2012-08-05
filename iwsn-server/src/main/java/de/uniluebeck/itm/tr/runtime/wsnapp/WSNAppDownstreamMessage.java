package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;

public class WSNAppDownstreamMessage {

	private final ImmutableSet<String> to;

	private final byte[] messageBytes;

	private final SettableFuture<WSNAppMessages.RequestStatus> future;

	public WSNAppDownstreamMessage(final ImmutableSet<String> to, final byte[] messageBytes) {
		this.to = to;
		this.messageBytes = messageBytes;
		this.future = SettableFuture.create();
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public ImmutableSet<String> getTo() {
		return to;
	}

	public SettableFuture<WSNAppMessages.RequestStatus> getFuture() {
		return future;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("to", to)
				.add("messageBytes", messageBytes.length + " bytes")
				.toString();
	}

}
