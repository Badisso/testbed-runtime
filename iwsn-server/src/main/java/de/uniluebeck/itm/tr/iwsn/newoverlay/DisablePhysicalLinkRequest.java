package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import static com.google.common.base.Preconditions.checkNotNull;

public class DisablePhysicalLinkRequest extends Request {

	private final NodeUrn to;

	@Inject
	DisablePhysicalLinkRequest(final RequestIdProvider requestIdProvider,
							   @Assisted("from") final NodeUrn from,
							   @Assisted("to") final NodeUrn to) {

		super(requestIdProvider, ImmutableSet.of(from));
		this.to = checkNotNull(to, "A node URN for request must not be null!");
	}

	public NodeUrn getFrom() {
		return nodeUrns.iterator().next();
	}

	public NodeUrn getTo() {
		return to;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("from", nodeUrns.iterator().next())
				.add("to", to)
				.toString();
	}
}
