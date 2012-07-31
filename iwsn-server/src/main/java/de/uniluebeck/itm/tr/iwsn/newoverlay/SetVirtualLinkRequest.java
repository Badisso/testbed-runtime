package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import static com.google.common.base.Preconditions.checkNotNull;

public class SetVirtualLinkRequest extends Request {

	private final NodeUrn to;

	public SetVirtualLinkRequest(final NodeUrn from, final NodeUrn to) {
		super(ImmutableSet.of(from));
		checkNotNull(to, "A node URN for a request must not be null!");
		this.to = to;
	}

	public NodeUrn getTo() {
		return to;
	}
}
