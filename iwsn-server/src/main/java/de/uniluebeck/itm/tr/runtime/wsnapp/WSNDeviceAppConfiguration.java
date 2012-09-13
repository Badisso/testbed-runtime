package de.uniluebeck.itm.tr.runtime.wsnapp;

import javax.annotation.Nullable;
import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class WSNDeviceAppConfiguration {

	private final String nodeUrn;

	private final String portalNodeUrn;

	@Nullable
	private File defaultImageFile;

	public WSNDeviceAppConfiguration(final String nodeUrn,
									 final String portalNodeUrn,
									 @Nullable final File defaultImageFile) {

		this.nodeUrn = checkNotNull(nodeUrn, "nodeUrn must not be null!");
		this.portalNodeUrn = checkNotNull(portalNodeUrn, "portalNodeUrn must not be null!");

		final boolean defaultImageFileNullOrReadableFile = defaultImageFile == null ||
				(defaultImageFile.exists() && defaultImageFile.isFile() && defaultImageFile.canRead());
		checkArgument(defaultImageFileNullOrReadableFile,
				"The default image for " + nodeUrn +
						" (\"" + (defaultImageFile != null ? defaultImageFile.getAbsolutePath() : "") + "\") "
						+ "either does not exists, is not a file or is not readable!"
		);
		this.defaultImageFile = defaultImageFile;
	}

	public String getNodeUrn() {
		return nodeUrn;
	}

	public String getPortalNodeUrn() {
		return portalNodeUrn;
	}

	@Nullable
	public File getDefaultImageFile() {
		return defaultImageFile;
	}
}
