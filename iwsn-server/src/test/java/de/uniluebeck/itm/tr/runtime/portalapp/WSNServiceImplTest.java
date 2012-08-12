package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppOverlayModule;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.ForwardingScheduledExecutorService;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import org.apache.log4j.Level;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WSNServiceImplTest {

	private static final NodeUrn NODE_URN_1 = new NodeUrn("urn:wisebed:uzl1:0x0001");

	private static final NodeUrn NODE_URN_2 = new NodeUrn("urn:wisebed:uzl1:0x0002");

	private static final String NODE_URN_1_STRING = NODE_URN_1.toString();

	private static final String NODE_URN_2_STRING = NODE_URN_2.toString();

	private static final ImmutableSet<NodeUrn> TWO_NODE_URNS = ImmutableSet.of(NODE_URN_1, NODE_URN_2);

	private static final List<String> TWO_NODE_URNS_LIST = newArrayList(NODE_URN_1_STRING, NODE_URN_2_STRING);

	private static DatatypeFactory DATATYPE_FACTORY;

	static {
		Logging.setLoggingDefaults(Level.OFF);
		try {
			DATATYPE_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw propagate(e);
		}
	}

	@Mock
	private OverlayEventBus eventBus;

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

	@Mock
	private Overlay overlay;

	@Mock
	private RequestIdProvider requestIdProvider;

	private WSNServiceImpl wsnService;

	private RequestFactory requestFactory;

	private ForwardingScheduledExecutorService scheduler;

	private class TestOverlayModule extends OverlayModule {

		@Override
		protected void configure() {
			super.configure();
			bind(Overlay.class).toInstance(overlay);
		}
	}

	@Before
	public void setUp() throws Exception {

		scheduler = new ForwardingScheduledExecutorService(
				Executors.newSingleThreadScheduledExecutor(),
				Executors.newCachedThreadPool()
		);

		final Injector injector = Guice.createInjector(
				new TestOverlayModule(),
				new PortalServerModule(scheduler)
		);

		requestFactory = injector.getInstance(RequestFactory.class);

		wsnService = new WSNServiceImpl(
				eventBus,
				requestFactory,
				deliveryManager,
				virtualLinkManager,
				requestIdProvider,
				config,
				preconditions
		);

		verify(eventBus, never()).register(wsnService);
		verify(eventBus, never()).unregister(wsnService);

		wsnService.startAndWait();

		verify(eventBus).register(wsnService);
		verify(eventBus, never()).unregister(wsnService);
	}

	@After
	public void tearDown() throws Exception {

		verify(eventBus).register(wsnService);
		verify(eventBus, never()).unregister(wsnService);

		wsnService.stopAndWait();

		verify(eventBus).unregister(wsnService);

		ExecutorUtils.shutdown(scheduler, 0, TimeUnit.SECONDS);
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

		final BackendNotificationsRequest request = requestFactory.createBackendNotificationsRequest(notifications);
		wsnService.onBackendNotificationsRequest(request);

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(deliveryManager).receiveNotification(captor.capture());
		final Set<String> capturedNotifications = ImmutableSet.copyOf(captor.getValue());
		assertTrue(Sets.difference(notifications, capturedNotifications).isEmpty());
	}

	@Test
	public void testIfAreNodesAliveWorks() throws Exception {

		final String requestId = wsnService.areNodesAlive(TWO_NODE_URNS_LIST);
		final ArgumentCaptor<AreNodesAliveRequest> req = ArgumentCaptor.forClass(AreNodesAliveRequest.class);

		verify(eventBus).post(req.capture());

		final AreNodesAliveRequest capReq = req.getValue();
		assertEquals(TWO_NODE_URNS, capReq.getNodeUrns());

		verifyThatResultIsForwardedCorrectlyOnSuccess(requestId, capReq);
	}

	@Test
	public void testIfDestroyVirtualLinkWorks() throws Exception {

		final String reqId = wsnService.destroyVirtualLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
		final ArgumentCaptor<DestroyVirtualLinkRequest> req = ArgumentCaptor.forClass(DestroyVirtualLinkRequest.class);

		verify(eventBus).post(req.capture());

		final DestroyVirtualLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq);

		verify(virtualLinkManager).removeVirtualLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
	}

	@Test
	public void testIfDisableNodeRequestWorks() throws Exception {

		final String reqId = wsnService.disableNode(NODE_URN_1_STRING);
		final ArgumentCaptor<DisableNodeRequest> req = ArgumentCaptor.forClass(DisableNodeRequest.class);

		verify(eventBus).post(req.capture());

		final DisableNodeRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getNodeUrn());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq);
	}

	@Test
	public void testIfDisablePhysicalLinkWorks() throws Exception {

		final String reqId = wsnService.disablePhysicalLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
		final ArgumentCaptor<DisablePhysicalLinkRequest> req =
				ArgumentCaptor.forClass(DisablePhysicalLinkRequest.class);

		verify(eventBus).post(req.capture());

		final DisablePhysicalLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq);
	}

	@Test
	public void testIfEnableNodeRequestWorks() throws Exception {

		final String reqId = wsnService.enableNode(NODE_URN_1_STRING);
		final ArgumentCaptor<EnableNodeRequest> req = ArgumentCaptor.forClass(EnableNodeRequest.class);

		verify(eventBus).post(req.capture());

		final EnableNodeRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getNodeUrn());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq);
	}

	@Test
	public void testIfEnablePhysicalLinkWorks() throws Exception {

		final String reqId = wsnService.enablePhysicalLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
		final ArgumentCaptor<EnablePhysicalLinkRequest> req = ArgumentCaptor.forClass(EnablePhysicalLinkRequest.class);

		verify(eventBus).post(req.capture());

		final EnablePhysicalLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq);
	}

	@Test
	public void testIfFlashImageWorks() throws Exception {
		// TODO implement
	}

	@Test
	public void testIfMessageDownstreamRequestsAreForwardedToNodes() throws Exception {
		// TODO implement
	}

	@Test
	public void testIfMessageUpstreamRequestsAreForwardedToClients() throws Exception {

		final DateTime now = new DateTime();
		final String time = DATATYPE_FACTORY.newXMLGregorianCalendar(now.toGregorianCalendar()).toXMLFormat();
		final byte[] image = {0, 1, 2};

		final MessageUpstreamRequest request = requestFactory.createMessageUpstreamRequest(NODE_URN_1, time, image);

		wsnService.onMessageUpstreamRequest(request);

		final ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(deliveryManager).receive(captor.capture());
		final Message capturedMessage = captor.getValue();

		assertEquals(NODE_URN_1_STRING, capturedMessage.getSourceNodeId());
		assertEquals(time, capturedMessage.getTimestamp().toXMLFormat());
		assertArrayEquals(image, capturedMessage.getBinaryData());
	}

	@Test
	public void testIfResetNodesWorks() throws Exception {
		// TODO implement
	}

	@Test
	public void testIfSetChannelPipelineWorks() throws Exception {
		// TODO implement
	}

	@Test
	public void testIfSetVirtualLinkWorks() throws Exception {

		final String remoteServiceInstance = "http://localhost:1234";
		final String reqId = wsnService.setVirtualLink(
				NODE_URN_1_STRING,
				NODE_URN_2_STRING,
				remoteServiceInstance,
				null,
				null
		);

		final ArgumentCaptor<SetVirtualLinkRequest> req = ArgumentCaptor.forClass(SetVirtualLinkRequest.class);

		verify(eventBus).post(req.capture());

		final SetVirtualLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq);

		verify(virtualLinkManager).addVirtualLink(NODE_URN_1_STRING, NODE_URN_2_STRING, remoteServiceInstance);
	}

	@SuppressWarnings("unchecked")
	private void verifyThatResultIsForwardedCorrectlyOnSuccess(final String issuedClientRequestId,
															   final Request request) {

		verify(deliveryManager, never()).receiveStatus(Matchers.<RequestStatus>any());

		request.getFuture().set(buildSuccessRequestResult(request));

		final ArgumentCaptor<List> responseCaptor = ArgumentCaptor.forClass(List.class);
		verify(deliveryManager).receiveStatus(responseCaptor.capture());

		final List<RequestStatus> capturedStatusList = responseCaptor.getValue();
		final RequestStatus capturedStatus = capturedStatusList.get(0);

		assertEquals(issuedClientRequestId, capturedStatus.getRequestId());
		assertEquals(1, capturedStatusList.size());
		assertEquals(request.getNodeUrns().size(), capturedStatusList.get(0).getStatus().size());

		Set<NodeUrn> receivedStatusNodeUrns = newHashSet();
		for (Status status : capturedStatusList.get(0).getStatus()) {
			receivedStatusNodeUrns.add(new NodeUrn(status.getNodeId()));
			assertEquals("", status.getMsg());
			assertEquals(1, (int) status.getValue());
		}

		assertTrue(Sets.difference(receivedStatusNodeUrns, request.getNodeUrns()).isEmpty());
	}

	private RequestResult buildSuccessRequestResult(final Request request) {
		final ImmutableMap.Builder<NodeUrn, Tuple<Integer, String>> resultMapBuilder = ImmutableMap.builder();
		for (NodeUrn nodeUrn : request.getNodeUrns()) {
			resultMapBuilder.put(nodeUrn, new Tuple<Integer, String>(1, ""));
		}
		return new RequestResult(request.getRequestId(), resultMapBuilder.build());
	}
}
