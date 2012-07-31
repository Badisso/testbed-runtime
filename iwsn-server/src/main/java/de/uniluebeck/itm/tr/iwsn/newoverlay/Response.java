package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

public class Response {

	protected final long requestId;

	protected final ImmutableMap<NodeUrn, Tuple<Boolean, String>> result;

	public Response(final long requestId, final ImmutableMap<NodeUrn, Tuple<Boolean, String>> result) {
		this.requestId = requestId;
		this.result = result;
	}

	public long getRequestId() {
		return requestId;
	}

	public ImmutableMap<NodeUrn, Tuple<Boolean, String>> getResult() {
		return result;
	}
}
