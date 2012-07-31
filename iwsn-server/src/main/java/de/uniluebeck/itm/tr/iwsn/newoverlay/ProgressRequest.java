package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProgressRequest {

	private final long progressRequestId;

	private final Request request;

	@Inject
	ProgressRequest(final Provider<Long> requestIdProvider,
					@Assisted final Request request) {

		this.progressRequestId = checkNotNull(requestIdProvider).get();
		this.request = checkNotNull(request);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("progressRequestId", progressRequestId)
				.add("request", request)
				.toString();
	}
}
