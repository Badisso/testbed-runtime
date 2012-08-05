package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.eventbus.EventBus;

public interface OverlayFactory {

	Overlay create(EventBus eventBus);

}
