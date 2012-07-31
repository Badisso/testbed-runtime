package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class FlashImageRequest extends Request {

	private final byte[] image;

	public FlashImageRequest(final ImmutableSet<NodeUrn> nodeUrns, byte[] image) {
		super(nodeUrns);

		checkNotNull(image, "A node image must not be null!");
		checkArgument(image.length > 0, "A node image must contain more than zero bytes!");

		this.image = image;
	}

	public byte[] getImage() {
		return image;
	}
}
