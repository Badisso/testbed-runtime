package de.uniluebeck.itm.tr.iwsn.gateway;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.devicedb.DeviceConfigDBModule;
import de.uniluebeck.itm.tr.iwsn.gateway.netty.NettyClientModule;
import de.uniluebeck.itm.tr.iwsn.nodeapi.NodeApiModule;
import de.uniluebeck.itm.wsn.deviceutils.ScheduledExecutorServiceModule;
import de.uniluebeck.itm.wsn.drivers.factories.DeviceFactoryModule;

import javax.inject.Singleton;

public class GatewayModule extends AbstractModule {

	private final GatewayConfig gatewayConfig;

	private GatewayScheduler gatewayScheduler;

	public GatewayModule(final GatewayConfig gatewayConfig) {
		this.gatewayConfig = gatewayConfig;
	}

	@Override
	protected void configure() {

		bind(GatewayConfig.class).toInstance(gatewayConfig);
		bind(GatewayEventBus.class).to(GatewayEventBusImpl.class).in(Scopes.SINGLETON);
		bind(GatewayDeviceManager.class).to(GatewayDeviceManagerImpl.class).in(Scopes.SINGLETON);
		bind(GatewayEventIdProvider.class).to(GatewayRandomEventIdProvider.class).in(Scopes.SINGLETON);
		bind(GatewayScheduler.class).to(GatewaySchedulerImpl.class).in(Scopes.SINGLETON);

		install(new NettyClientModule());
		install(new DeviceFactoryModule());
		install(new DeviceConfigDBModule());
		install(new NodeApiModule());
		install(new ScheduledExecutorServiceModule("GatewayScheduler"));
		install(new FactoryModuleBuilder()
				.implement(GatewayDeviceAdapter.class, GatewaySingleDeviceAdapterImpl.class)
				.build(GatewayDeviceAdapterFactory.class)
		);
	}

	@Provides
	@Singleton
	EventBus provideEventBus() {
		return new EventBus("GatewayEventBus");
	}
}
