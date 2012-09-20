package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DestroyVirtualLinksRequest extends Request {

	final ImmutableMap<NodeUrn, NodeUrn> links;

	@Inject
	DestroyVirtualLinksRequest(final RequestIdProvider requestIdProvider,
							   @Assisted final ImmutableMap<NodeUrn, NodeUrn> links) {

		super(requestIdProvider, links.keySet());
		this.links = checkNotNull(links, "Argument links must not be null");
		checkArgument(!links.keySet().isEmpty(), "At least one link must be provided");
	}

	public ImmutableMap<NodeUrn, NodeUrn> getLinks() {
		return links;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("links", links)
				.toString();
	}
}
