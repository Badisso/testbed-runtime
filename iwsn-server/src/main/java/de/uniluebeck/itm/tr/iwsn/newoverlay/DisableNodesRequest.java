package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class DisableNodesRequest extends Request {

	@Inject
	DisableNodesRequest(final RequestIdProvider requestIdProvider,
						@Assisted final ImmutableSet<NodeUrn> nodeUrns) {

		super(requestIdProvider, nodeUrns);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("nodeUrns", futureMap.keySet())
				.toString();
	}
}
