package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class ResetNodesRequest extends Request {

	public ResetNodesRequest(final ImmutableSet<NodeUrn> nodeUrns) {
		super(nodeUrns);
	}
}
