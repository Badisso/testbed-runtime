package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayModule;
import de.uniluebeck.itm.tr.iwsn.newoverlay.RequestFactory;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.util.Logging;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WSNServiceImplTest {

	static {
		Logging.setLoggingDefaults();
	}

	private EventBus eventBus;

	@Mock
	private TestbedRuntime testbedRuntime;

	@Mock
	private DeliveryManager deliveryManager;

	@Mock
	private WSNServiceVirtualLinkManager virtualLinkManager;

	@Mock
	private WSNServiceConfig config;

	@Mock
	private WSNPreconditions preconditions;

	private WSNServiceImpl wsnService;

	private RequestFactory requestFactory;

	@Before
	public void setUp() throws Exception {

		eventBus = spy(new EventBus());
		final Injector injector = Guice.createInjector(new OverlayModule());
		requestFactory = injector.getInstance(RequestFactory.class);
		final Provider<Long> requestIdProvider = injector.getProvider(Long.class);

		doCallRealMethod().when(eventBus).post(Matchers.<Object>any());
		doCallRealMethod().when(eventBus).register(Matchers.<Object>any());
		doCallRealMethod().when(eventBus).unregister(Matchers.<Object>any());

		wsnService = new WSNServiceImpl(
				eventBus,
				requestFactory,
				deliveryManager,
				virtualLinkManager,
				requestIdProvider,
				config,
				preconditions
		);

		wsnService.startAndWait();
	}

	@Test
	public void testIfServiceRegistersWithEventBusOnStartup() throws Exception {
		verify(eventBus).register(wsnService);
		verify(eventBus, never()).unregister(wsnService);
	}

	@Test
	public void testIfServiceUnregistersWithEventBusOnShutdown() throws Exception {
		verify(eventBus, never()).unregister(wsnService);
		wsnService.stopAndWait();
		verify(eventBus).unregister(wsnService);
	}

	@Test
	public void testAddingControllerShouldPassItToDeliveryManager() throws Exception {

		final String controllerEndpointUrl = "http://localhost:1234/endpoint";

		verify(deliveryManager, never()).addController(Matchers.<String>any());
		wsnService.addController(controllerEndpointUrl);
		verify(deliveryManager).addController(eq(controllerEndpointUrl));
	}

	@Test
	public void testRemovingControllerShouldRemoveItFromDeliveryManager() throws Exception {

		final String controllerEndpointUrl = "http://localhost:1234/endpoint";

		verify(deliveryManager, never()).removeController(Matchers.<String>any());
		wsnService.removeController(controllerEndpointUrl);
		verify(deliveryManager).removeController(eq(controllerEndpointUrl));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBackendNotificationsShouldBePassedToDeliveryManager() throws Exception {

		final ImmutableSet<String> notifications = ImmutableSet.of("hello", "world", "!");

		verify(deliveryManager, never()).receiveNotification(Matchers.<List<String>>any());

		eventBus.post(requestFactory.createBackendNotificationsRequest(notifications));
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

		verify(deliveryManager).receiveNotification(captor.capture());
		final Set<String> capturedNotifications = ImmutableSet.copyOf(captor.getValue());
		assertTrue(Sets.difference(notifications, capturedNotifications).isEmpty());
	}
}
