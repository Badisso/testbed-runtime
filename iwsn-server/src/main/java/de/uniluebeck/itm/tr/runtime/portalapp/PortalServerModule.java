package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;

import java.util.concurrent.ScheduledExecutorService;

public class PortalServerModule extends AbstractModule {

	private final ScheduledExecutorService virtualLinkManagerScheduledExecutorService;

	public PortalServerModule(final ScheduledExecutorService virtualLinkManagerScheduledExecutorService) {
		this.virtualLinkManagerScheduledExecutorService = virtualLinkManagerScheduledExecutorService;
	}

	@Override
	protected void configure() {

		bind(ScheduledExecutorService.class).toInstance(virtualLinkManagerScheduledExecutorService);

		install(new FactoryModuleBuilder()
				.implement(SessionManagementService.class, SessionManagementServiceImpl.class)
				.build(SessionManagementServiceFactory.class)
		);

		install(new FactoryModuleBuilder().build(SessionManagementSoapServiceFactory.class));

		install(new FactoryModuleBuilder()
				.implement(WSNServiceHandle.class, WSNServiceHandleImpl.class)
				.build(WSNServiceHandleFactory.class)
		);

		install(new FactoryModuleBuilder()
				.implement(WSNService.class, WSNServiceImpl.class)
				.build(WSNServiceFactory.class)
		);
	}
}
