package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import javax.inject.Provider;

public class DevicesAttachedEventRequest extends Request {

	@Inject
	DevicesAttachedEventRequest(final Provider<Long> requestIdProvider,
								@Assisted final ImmutableSet<NodeUrn> nodeUrns) {

		super(requestIdProvider, nodeUrns);
	}

	public ImmutableSet<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}
}
