package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import javax.inject.Inject;
import javax.inject.Provider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Request {

	protected final long requestId;

	protected final ImmutableSet<NodeUrn> nodeUrns;

	@Inject
	Request(final Provider<Long> requestIdProvider, final ImmutableSet<NodeUrn> nodeUrns) {

		checkNotNull(requestIdProvider);
		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.size() > 0, "A request must at least contain one node URN!");
		for (NodeUrn nodeUrn : nodeUrns) {
			checkNotNull(nodeUrn, "A node URN for a request must not be null!");
		}

		this.nodeUrns = nodeUrns;
		this.requestId = requestIdProvider.get();
	}

	public ImmutableSet<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("nodeUrns", nodeUrns)
				.toString();
	}
}
