package de.uniluebeck.itm.tr.iwsn;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class NodeUrnPrefix {

	public final static Pattern URN_PREFIX_PATTERN = Pattern.compile(
			"^urn:[a-z0-9][a-z0-9-]{0,31}((:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+)|(:))$",
			Pattern.CASE_INSENSITIVE
	);

	private String nodeUrnPrefix;

	public NodeUrnPrefix(final String nodeUrnPrefix) {

		checkNotNull(nodeUrnPrefix);
		checkArgument(URN_PREFIX_PATTERN.matcher(nodeUrnPrefix).matches());
		checkArgument(nodeUrnPrefix.endsWith(":"));

		this.nodeUrnPrefix = nodeUrnPrefix;
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final NodeUrnPrefix that = (NodeUrnPrefix) o;

		return nodeUrnPrefix.equals(that.nodeUrnPrefix);
	}

	@Override
	public int hashCode() {
		return nodeUrnPrefix.hashCode();
	}
}
