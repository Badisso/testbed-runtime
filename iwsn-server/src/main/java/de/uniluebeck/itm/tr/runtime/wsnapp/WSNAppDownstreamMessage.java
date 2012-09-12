package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;

public class WSNAppDownstreamMessage {

	private final ImmutableSet<String> to;

	private final byte[] messageBytes;

	private final ImmutableMap<String, SettableFuture<Void>> futureMap;

	public WSNAppDownstreamMessage(final ImmutableSet<String> to, final byte[] messageBytes) {

		this.to = to;
		this.messageBytes = messageBytes;

		final ImmutableMap.Builder<String, SettableFuture<Void>> futureMapBuilder = ImmutableMap.builder();
		for (String t : to) {
			futureMapBuilder.put(t, SettableFuture.<Void>create());
		}
		this.futureMap = futureMapBuilder.build();
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public ImmutableSet<String> getTo() {
		return to;
	}

	public ImmutableMap<String, SettableFuture<Void>> getFutureMap() {
		return futureMap;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("to", to)
				.add("messageBytes", messageBytes.length + " bytes")
				.toString();
	}

}
