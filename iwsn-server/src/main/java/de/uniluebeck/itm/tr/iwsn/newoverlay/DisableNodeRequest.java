package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class DisableNodeRequest extends Request {

	public DisableNodeRequest(final NodeUrn nodeUrn) {
		super(ImmutableSet.of(nodeUrn));
	}
}
