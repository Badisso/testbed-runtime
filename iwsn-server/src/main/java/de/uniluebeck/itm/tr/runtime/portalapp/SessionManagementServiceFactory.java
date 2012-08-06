package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;

import java.util.concurrent.ScheduledExecutorService;

public interface SessionManagementServiceFactory {

	SessionManagementService create(SessionManagementServiceConfig config,
									SessionManagementPreconditions preconditions,
									ScheduledExecutorService scheduler);

}
