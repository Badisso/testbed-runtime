package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FlashImageRequest extends Request {

	private final byte[] image;

	@Inject
	FlashImageRequest(final RequestIdProvider requestIdProvider,
					  @Assisted final ImmutableSet<NodeUrn> nodeUrns,
					  @Assisted byte[] image) {

		super(requestIdProvider, nodeUrns);

		checkNotNull(image, "A node image must not be null!");
		checkArgument(image.length > 0, "A node image must contain more than zero bytes!");

		this.image = image;
	}

	public ImmutableSet<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}

	public byte[] getImage() {
		return image;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("requestId", requestId)
				.add("nodeUrns", nodeUrns)
				.add("image", image.length + " bytes")
				.toString();
	}
}
