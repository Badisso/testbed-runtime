package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Request {

	protected final ImmutableSet<NodeUrn> nodeUrns;

	public Request(final ImmutableSet<NodeUrn> nodeUrns) {

		checkNotNull(nodeUrns);
		checkArgument(nodeUrns.size() > 0, "A request must at least contain one node URN!");
		for (NodeUrn nodeUrn : nodeUrns) {
			checkNotNull(nodeUrn, "A node URN for a request must not be null!");
		}

		this.nodeUrns = nodeUrns;
	}

	public ImmutableSet<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}
}
