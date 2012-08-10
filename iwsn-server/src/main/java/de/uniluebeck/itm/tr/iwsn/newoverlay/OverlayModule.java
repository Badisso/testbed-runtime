package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OverlayModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(RequestIdProvider.class).to(RequestIdProviderImpl.class);
		install(new FactoryModuleBuilder().build(RequestFactory.class));
	}
}
