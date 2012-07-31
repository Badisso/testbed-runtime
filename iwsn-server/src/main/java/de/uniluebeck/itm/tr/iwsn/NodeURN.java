package de.uniluebeck.itm.tr.iwsn;

import de.uniluebeck.itm.tr.util.StringUtils;

import java.net.URI;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class NodeUrn {

	public final static Pattern URN_PATTERN = Pattern.compile(
			"^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$",
			Pattern.CASE_INSENSITIVE
	);

	private URI uri;

	public NodeUrn(final String nodeUrn) {

		checkNotNull(nodeUrn);
		checkArgument(URN_PATTERN.matcher(nodeUrn).matches());
		checkArgument(StringUtils.hasHexOrDecLongUrnSuffix(nodeUrn));

		this.uri = URI.create(nodeUrn);
	}

	@Override
	public boolean equals(final Object o) {

		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final NodeUrn nodeUrn = (NodeUrn) o;

		return uri.equals(nodeUrn.uri);
	}

	@Override
	public int hashCode() {
		return uri.hashCode();
	}

	@Override
	public String toString() {
		return uri.toString();
	}
}
