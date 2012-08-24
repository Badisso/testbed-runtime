package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.ProgressSettableFuture;

import javax.annotation.Nullable;

import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

public class Request {

	protected final long requestId;

	protected final ImmutableMap<NodeUrn, ProgressSettableFuture<Void>> futureMap;

	protected Request(final RequestIdProvider requestIdProvider,
					  @Nullable final ImmutableSet<NodeUrn> nodeUrns) {

		checkNotNull(requestIdProvider);

		if (nodeUrns != null) {
			for (NodeUrn nodeUrn : nodeUrns) {
				checkNotNull(nodeUrn, "A node URN for a request must not be null!");
			}
		}

		final ImmutableMap.Builder<NodeUrn, ProgressSettableFuture<Void>> futureMapBuilder = ImmutableMap.builder();

		if (nodeUrns != null) {

			for (NodeUrn nodeUrn : nodeUrns) {
				futureMapBuilder.put(nodeUrn, ProgressSettableFuture.<Void>create());
			}
		}

		this.futureMap = futureMapBuilder.build();
		this.requestId = requestIdProvider.get();
	}

	@VisibleForTesting
	public ImmutableSet<NodeUrn> getNodeUrns() {
		return futureMap.keySet();
	}

	public long getRequestId() {
		return requestId;
	}

	public ListenableFuture<ImmutableMap<NodeUrn, ProgressSettableFuture<Void>>> getFuture() {
		final SettableFuture<ImmutableMap<NodeUrn, ProgressSettableFuture<Void>>> future = SettableFuture.create();
		Futures.allAsList(getFutureMap().values()).addListener(new Runnable() {
			@Override
			public void run() {
				future.set(futureMap);
			}
		}, MoreExecutors.sameThreadExecutor());
		return future;
	}

	public ImmutableMap<NodeUrn, ProgressSettableFuture<Void>> getFutureMap() {
		return futureMap;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("nodeUrns", getNodeUrns())
				.toString();
	}
}
