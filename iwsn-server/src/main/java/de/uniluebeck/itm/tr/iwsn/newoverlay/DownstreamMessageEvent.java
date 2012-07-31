package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class DownstreamMessageEvent {

	private final ImmutableSet<NodeUrn> to;

	private final byte[] messageBytes;

	public DownstreamMessageEvent(final ImmutableSet<NodeUrn> to, final byte[] messageBytes) {
		this.to = to;
		this.messageBytes = messageBytes;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public ImmutableSet<NodeUrn> getTo() {
		return to;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("to", to)
				.add("messageBytes", messageBytes.length + " bytes")
				.toString();
	}
}
