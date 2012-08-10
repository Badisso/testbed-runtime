package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;

import java.util.concurrent.ScheduledExecutorService;

public interface SessionManagementSoapServiceFactory {

	SessionManagementSoapService create(SessionManagementService service,
										SessionManagementServiceConfig config,
										SessionManagementPreconditions preconditions,
										DeliveryManager deliveryManager,
										ScheduledExecutorService scheduledExecutorService);

}
