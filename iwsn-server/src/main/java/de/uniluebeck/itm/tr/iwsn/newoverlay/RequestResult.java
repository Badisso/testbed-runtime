package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

import javax.annotation.Nullable;

public class RequestResult {

	protected final long requestId;

	protected final ImmutableMap<NodeUrn, Tuple<Integer, String>> result;

	public RequestResult(final long requestId, @Nullable final ImmutableMap<NodeUrn, Tuple<Integer, String>> result) {
		this.requestId = requestId;
		this.result = result;
	}

	public long getRequestId() {
		return requestId;
	}

	@Nullable
	public ImmutableMap<NodeUrn, Tuple<Integer, String>> getResult() {
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
