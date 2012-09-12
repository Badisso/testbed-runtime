package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.util.concurrent.Service;

public interface Testbed extends Service {

	TestbedEventBus getEventBus();

}
