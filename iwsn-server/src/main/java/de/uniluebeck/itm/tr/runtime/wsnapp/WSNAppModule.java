package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;

public class WSNAppModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
				.implement(WSNApp.class, WSNAppImpl.class)
				.build(WSNAppFactory.class)
		);
	}
}
