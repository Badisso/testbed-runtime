package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class AreNodesAliveSmRequest extends Request {

	public AreNodesAliveSmRequest(final ImmutableSet<NodeUrn> nodeUrns) {
		super(nodeUrns);
	}
}
