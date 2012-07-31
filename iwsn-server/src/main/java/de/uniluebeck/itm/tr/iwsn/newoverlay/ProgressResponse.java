package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class ProgressResponse {

	private final long progressRequestId;

	private final long requestId;

	private ImmutableMap<NodeUrn, Integer> progressMap;

	public ProgressResponse(final long progressRequestId,
							final long requestId,
							final ImmutableMap<NodeUrn, Integer> progressMap) {

		this.progressRequestId = progressRequestId;
		this.requestId = requestId;
		this.progressMap = progressMap;
	}

	public ImmutableMap<NodeUrn, Integer> getProgressMap() {
		return progressMap;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("progressRequestId", progressRequestId)
				.add("requestId", requestId)
				.add("progressMap", progressMap)
				.toString();
	}
}
