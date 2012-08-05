package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;

public class Response {

	protected final long requestId;

	protected final RequestResult result;

	public Response(final long requestId, final RequestResult result) {
		this.requestId = requestId;
		this.result = result;
	}

	public long getRequestId() {
		return requestId;
	}

	public RequestResult getResult() {
		return result;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("result", result)
				.toString();
	}
}