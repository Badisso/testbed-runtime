package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import org.joda.time.DateTime;

import javax.inject.Provider;

public class MessageUpstreamRequest extends Request {

	private final String timestamp;

	private final byte[] messageBytes;

	@Inject
	MessageUpstreamRequest(final RequestIdProvider requestIdProvider,
						   @Assisted final NodeUrn from,
						   @Assisted final String timestamp,
						   @Assisted final byte[] messageBytes) {

		super(requestIdProvider, ImmutableSet.of(from));

		this.timestamp = timestamp;
		this.messageBytes = messageBytes;
	}

	public NodeUrn getFrom() {
		return futureMap.keySet().iterator().next();
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public String getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("from", futureMap.keySet().iterator().next())
				.add("timestamp", timestamp)
				.add("messageBytes", messageBytes.length + " bytes")
				.toString();
	}
}
