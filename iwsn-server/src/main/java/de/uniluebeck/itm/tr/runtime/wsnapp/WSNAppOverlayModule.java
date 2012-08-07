package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayFactory;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayModule;

public class WSNAppOverlayModule extends OverlayModule {

	private final WSNApp wsnApp;

	public WSNAppOverlayModule(final WSNApp wsnApp) {
		this.wsnApp = wsnApp;
	}

	@Override
	protected void configure() {

		super.configure();

		bind(WSNApp.class).toInstance(wsnApp);

		install(new FactoryModuleBuilder()
				.implement(Overlay.class, WSNAppOverlay.class)
				.build(OverlayFactory.class)
		);
	}
}
