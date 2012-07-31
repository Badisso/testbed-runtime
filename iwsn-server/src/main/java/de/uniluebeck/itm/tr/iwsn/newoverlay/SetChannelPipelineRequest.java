package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SetChannelPipelineRequest extends Request {

	private final List<Tuple<String, Map<String, String>>> pipeline;

	public SetChannelPipelineRequest(final ImmutableSet<NodeUrn> nodeUrns,
									 final List<Tuple<String, Map<String, String>>> pipeline) {
		super(nodeUrns);

		checkNotNull(pipeline, "A node URN for a request must not be null!");
		for (Tuple<String, Map<String, String>> tuple : pipeline) {
			checkNotNull(tuple, "All pipeline entries must not be null!");
		}

		this.pipeline = pipeline;
	}

	public List<Tuple<String, Map<String, String>>> getPipeline() {
		return pipeline;
	}
}
