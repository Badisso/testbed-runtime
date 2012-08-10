package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

import javax.inject.Provider;

public class BackendNotificationsRequest extends Request {

	private final ImmutableSet<String> notifications;

	@Inject
	BackendNotificationsRequest(final RequestIdProvider requestIdProvider,
								@Assisted final ImmutableSet<String> notifications) {
		super(requestIdProvider, ImmutableSet.<NodeUrn>of());
		this.notifications = notifications;
	}

	public ImmutableSet<String> getNotifications() {
		return notifications;
	}
}
