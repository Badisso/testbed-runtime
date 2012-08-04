package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;

import java.util.concurrent.TimeUnit;

public interface Overlay extends Service {

	EventBus getEventBus();

}
