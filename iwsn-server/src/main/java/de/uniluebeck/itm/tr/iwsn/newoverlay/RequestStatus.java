package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

public class RequestStatus {

	private final long requestId;

	private ImmutableMap<NodeUrn, Tuple<Integer, String>> status;

	public RequestStatus(final long requestId, final ImmutableMap<NodeUrn, Tuple<Integer, String>> status) {
		this.requestId = requestId;
		this.status = status;
	}

	public long getRequestId() {
		return requestId;
	}

	public ImmutableMap<NodeUrn, Tuple<Integer, String>> getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("status", status)
				.toString();
	}
}
