package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;

public class EventBusTest {

	private EventBus eventBus = new EventBus();

	public EventBusTest() {
		eventBus.register(this);
	}

	@Subscribe
	public void onRequest(Request request) {
		System.out.println("EventBusTest.onRequest(" + request + ")");
	}

	@Subscribe
	public void onAreNodesAliveRequest(AreNodesAliveRequest request) {
		System.out.println("EventBusTest.onAreNodesAliveRequest(" + request + ")");
	}

	public void post() {
		eventBus.post(
				new AreNodesAliveRequest(
						new Provider<Long>() {
							@Override
							public Long get() {
								return 1L;
							}
						},
						ImmutableSet.of(new NodeUrn("urn:wisebed:uz1l:0x1234"))
				)
		);
	}

	public static void main(String[] args) {
		new EventBusTest().post();
	}

}
