package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import org.joda.time.DateTime;

public class UpstreamMessageEvent {

	private final NodeUrn from;

	private final DateTime timestamp;

	private final byte[] messageBytes;

	public UpstreamMessageEvent(final NodeUrn from, final DateTime timestamp, final byte[] messageBytes) {
		this.from = from;
		this.timestamp = timestamp;
		this.messageBytes = messageBytes;
	}

	public NodeUrn getFrom() {
		return from;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public DateTime getTimestamp() {
		return timestamp;
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
