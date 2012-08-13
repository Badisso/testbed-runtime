package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

import static com.google.common.base.Preconditions.checkNotNull;

public class SetChannelPipelineRequest extends Request {

	private final ImmutableList<Tuple<String, ImmutableMap<String, String>>> pipeline;

	@Inject
	SetChannelPipelineRequest(final RequestIdProvider requestIdProvider,
							  @Assisted final ImmutableSet<NodeUrn> nodeUrns,
							  @Assisted final ImmutableList<Tuple<String, ImmutableMap<String, String>>> pipeline) {

		super(requestIdProvider, nodeUrns);

		checkNotNull(pipeline, "A node URN for a request must not be null!");
		for (Tuple<String, ImmutableMap<String, String>> tuple : pipeline) {
			checkNotNull(tuple, "All pipeline entries must not be null!");
			checkNotNull(tuple.getFirst(), "All pipeline entry names must not be null!");
		}

		this.pipeline = pipeline;
	}

	public ImmutableSet<NodeUrn> getNodeUrns() {
		return futureMap.keySet();
	}

	public ImmutableList<Tuple<String, ImmutableMap<String, String>>> getPipeline() {
		return pipeline;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("nodeUrns", futureMap.keySet())
				.add("pipeline", pipeline)
				.toString();
	}
}
