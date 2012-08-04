package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import javax.inject.Provider;

public class MessageDownstreamRequest extends Request {

	private final byte[] messageBytes;

	@Inject
	MessageDownstreamRequest(final Provider<Long> requestIdProvider,
							 @Assisted final ImmutableSet<NodeUrn> to,
							 @Assisted final byte[] messageBytes) {

		super(requestIdProvider, to);

		this.messageBytes = messageBytes;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public ImmutableSet<NodeUrn> getTo() {
		return nodeUrns;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("to", nodeUrns)
				.add("messageBytes", messageBytes.length + " bytes")
				.toString();
	}
}
