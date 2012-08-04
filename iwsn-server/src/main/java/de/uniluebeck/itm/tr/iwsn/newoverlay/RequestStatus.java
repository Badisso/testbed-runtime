package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class RequestStatus {

	private final long requestId;

	private ImmutableMap<NodeUrn, Integer> status;

	public RequestStatus(final long requestId, final ImmutableMap<NodeUrn, Integer> status) {
		this.requestId = requestId;
		this.status = status;
	}

	public long getRequestId() {
		return requestId;
	}

	public ImmutableMap<NodeUrn, Integer> getStatus() {
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
