package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class AreNodesAliveRequest extends Request {

	public AreNodesAliveRequest(final ImmutableSet<NodeUrn> nodeUrns) {
		super(nodeUrns);
	}
}
