package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class EnableNodeRequest extends Request {

	@Inject
	EnableNodeRequest(final Provider<Long> requestIdProvider,
					  @Assisted final NodeUrn nodeUrn) {

		super(requestIdProvider, ImmutableSet.of(nodeUrn));
	}

	public NodeUrn getNodeUrn() {
		return nodeUrns.iterator().next();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("nodeUrn", nodeUrns.iterator().next())
				.toString();
	}
}
