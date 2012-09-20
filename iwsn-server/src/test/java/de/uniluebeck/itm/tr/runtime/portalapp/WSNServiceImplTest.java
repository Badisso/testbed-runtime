package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.*;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.ForwardingScheduledExecutorService;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.wsn.Program;
import org.apache.log4j.Level;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WSNServiceImplTest {

	private static final NodeUrn NODE_URN_1 = new NodeUrn("urn:wisebed:uzl1:0x0001");

	private static final NodeUrn NODE_URN_2 = new NodeUrn("urn:wisebed:uzl1:0x0002");

	private static final NodeUrn NODE_URN_3 = new NodeUrn("urn:wisebed:uzl1:0x0003");

	private static final NodeUrn NODE_URN_4 = new NodeUrn("urn:wisebed:uzl1:0x0004");

	private static final NodeUrn NODE_URN_5 = new NodeUrn("urn:wisebed:uzl1:0x0005");

	private static final String NODE_URN_1_STRING = NODE_URN_1.toString();

	private static final String NODE_URN_2_STRING = NODE_URN_2.toString();

	private static final String NODE_URN_3_STRING = NODE_URN_3.toString();

	private static final String NODE_URN_4_STRING = NODE_URN_4.toString();

	private static final String NODE_URN_5_STRING = NODE_URN_5.toString();

	private static final Program PROGRAM_1 = new Program();

	private static final Program PROGRAM_2 = new Program();

	private static final Program PROGRAM_3 = new Program();

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

		PROGRAM_1.setProgram(new byte[]{1, 2, 3});
		PROGRAM_2.setProgram(new byte[]{2, 3, 4});
		PROGRAM_3.setProgram(new byte[]{4, 5, 6});
	}

	@Mock
	private TestbedEventBus testbedEventBus;

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
	private Testbed testbed;

	@Mock
	private RequestIdProvider requestIdProvider;

	private WSNServiceImpl wsnService;

	private RequestFactory requestFactory;

	private ForwardingScheduledExecutorService scheduler;

	private class TestTestbedModule extends TestbedModule {

		@Override
		protected void configure() {
			super.configure();
			bind(Testbed.class).toInstance(testbed);
		}
	}

	@Before
	public void setUp() throws Exception {

		scheduler = new ForwardingScheduledExecutorService(
				Executors.newSingleThreadScheduledExecutor(),
				Executors.newCachedThreadPool()
		);

		final Injector injector = Guice.createInjector(
				new TestTestbedModule(),
				new PortalServerModule(scheduler)
		);

		requestFactory = injector.getInstance(RequestFactory.class);

		wsnService = new WSNServiceImpl(
				testbedEventBus,
				requestFactory,
				deliveryManager,
				virtualLinkManager,
				requestIdProvider,
				config,
				preconditions
		);

		verify(testbedEventBus, never()).register(wsnService);
		verify(testbedEventBus, never()).unregister(wsnService);

		wsnService.startAndWait();

		verify(testbedEventBus).register(wsnService);
		verify(testbedEventBus, never()).unregister(wsnService);

		when(requestIdProvider.get()).thenReturn(new Random().nextLong());
	}

	@After
	public void tearDown() throws Exception {

		verify(testbedEventBus).register(wsnService);
		verify(testbedEventBus, never()).unregister(wsnService);

		wsnService.stopAndWait();

		verify(testbedEventBus).unregister(wsnService);

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

		verify(testbedEventBus).post(req.capture());

		final AreNodesAliveRequest capReq = req.getValue();
		assertEquals(TWO_NODE_URNS, capReq.getNodeUrns());

		verifyThatResultIsForwardedCorrectlyOnSuccess(requestId, capReq, 1);
	}

	@Test
	public void testIfDestroyVirtualLinkWorks() throws Exception {

		final String reqId = wsnService.destroyVirtualLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
		final ArgumentCaptor<DestroyVirtualLinksRequest> req = ArgumentCaptor.forClass(DestroyVirtualLinksRequest.class);

		verify(testbedEventBus).post(req.capture());

		final DestroyVirtualLinksRequest capReq = req.getValue();
		assertTrue(capReq.getLinks().containsKey(NODE_URN_1));
		assertEquals(NODE_URN_2, capReq.getLinks().get(NODE_URN_1));

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);

		verify(virtualLinkManager).removeVirtualLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
	}

	@Test
	public void testIfDisableNodeRequestWorks() throws Exception {

		final String reqId = wsnService.disableNode(NODE_URN_1_STRING);
		final ArgumentCaptor<DisableNodeRequest> req = ArgumentCaptor.forClass(DisableNodeRequest.class);

		verify(testbedEventBus).post(req.capture());

		final DisableNodeRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getNodeUrn());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);
	}

	@Test
	public void testIfDisablePhysicalLinkWorks() throws Exception {

		final String reqId = wsnService.disablePhysicalLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
		final ArgumentCaptor<DisablePhysicalLinkRequest> req =
				ArgumentCaptor.forClass(DisablePhysicalLinkRequest.class);

		verify(testbedEventBus).post(req.capture());

		final DisablePhysicalLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);
	}

	@Test
	public void testIfEnableNodeRequestWorks() throws Exception {

		final String reqId = wsnService.enableNode(NODE_URN_1_STRING);
		final ArgumentCaptor<EnableNodeRequest> req = ArgumentCaptor.forClass(EnableNodeRequest.class);

		verify(testbedEventBus).post(req.capture());

		final EnableNodeRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getNodeUrn());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);
	}

	@Test
	public void testIfEnablePhysicalLinkWorks() throws Exception {

		final String reqId = wsnService.enablePhysicalLink(NODE_URN_1_STRING, NODE_URN_2_STRING);
		final ArgumentCaptor<EnablePhysicalLinkRequest> req = ArgumentCaptor.forClass(EnablePhysicalLinkRequest.class);

		verify(testbedEventBus).post(req.capture());

		final EnablePhysicalLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);
	}

	@Test
	public void testIfFlashImageWorks() throws Exception {

		final List<String> nodeIds = newArrayList(
				NODE_URN_1_STRING,
				NODE_URN_2_STRING,
				NODE_URN_3_STRING,
				NODE_URN_4_STRING,
				NODE_URN_5_STRING
		);

		final List<Program> programs = newArrayList(PROGRAM_1, PROGRAM_2, PROGRAM_3);
		final List<Integer> programIndices = newArrayList(0, 1, 1, 2, 0);

		final String reqId = wsnService.flashPrograms(nodeIds, programIndices, programs);
		final ArgumentCaptor<FlashImageRequest> req = ArgumentCaptor.forClass(FlashImageRequest.class);

		verify(testbedEventBus, times(3)).post(req.capture());

		final List<FlashImageRequest> capturedRequests = req.getAllValues();
		assertEquals(3, capturedRequests.size());

		assertEquals(ImmutableSet.of(NODE_URN_1, NODE_URN_5), capturedRequests.get(0).getNodeUrns());
		assertEquals(ImmutableSet.of(NODE_URN_2, NODE_URN_3), capturedRequests.get(1).getNodeUrns());
		assertEquals(ImmutableSet.of(NODE_URN_4), capturedRequests.get(2).getNodeUrns());

		assertArrayEquals(PROGRAM_1.getProgram(), capturedRequests.get(0).getImage());
		assertArrayEquals(PROGRAM_2.getProgram(), capturedRequests.get(1).getImage());
		assertArrayEquals(PROGRAM_3.getProgram(), capturedRequests.get(2).getImage());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capturedRequests.get(0), 100);
		reset(deliveryManager);
		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capturedRequests.get(1), 100);
		reset(deliveryManager);
		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capturedRequests.get(2), 100);
		reset(deliveryManager);
	}

	@Test
	public void testIfMessageDownstreamRequestsAreForwardedToNodes() throws Exception {

		final byte[] messageBytes = {1, 2, 3};

		final Message message = new Message();
		message.setBinaryData(messageBytes);
		message.setSourceNodeId("i:dont:care");
		message.setTimestamp(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));

		wsnService.send(newArrayList(NODE_URN_1_STRING, NODE_URN_3_STRING), message);

		final ArgumentCaptor<MessageDownstreamRequest> req = ArgumentCaptor.forClass(MessageDownstreamRequest.class);

		verify(testbedEventBus).post(req.capture());

		final MessageDownstreamRequest capReq = req.getValue();
		assertEquals(ImmutableSet.of(NODE_URN_1, NODE_URN_3), capReq.getTo());
		assertArrayEquals(messageBytes, capReq.getMessageBytes());
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

		final String reqId = wsnService.resetNodes(
				newArrayList(NODE_URN_1_STRING, NODE_URN_3_STRING, NODE_URN_5_STRING)
		);

		final ArgumentCaptor<ResetNodesRequest> req = ArgumentCaptor.forClass(ResetNodesRequest.class);

		verify(testbedEventBus).post(req.capture());

		final ResetNodesRequest capReq = req.getValue();
		assertEquals(ImmutableSet.of(NODE_URN_1, NODE_URN_3, NODE_URN_5), capReq.getNodeUrns());
		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);
	}

	@Test
	@Ignore("To be implemented...")
	public void testIfSetChannelPipelineWorks() throws Exception {
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

		verify(testbedEventBus).post(req.capture());

		final SetVirtualLinkRequest capReq = req.getValue();
		assertEquals(NODE_URN_1, capReq.getFrom());
		assertEquals(NODE_URN_2, capReq.getTo());

		verifyThatResultIsForwardedCorrectlyOnSuccess(reqId, capReq, 1);

		verify(virtualLinkManager).addVirtualLink(NODE_URN_1_STRING, NODE_URN_2_STRING, remoteServiceInstance);
	}

	@SuppressWarnings("unchecked")
	private void verifyThatResultIsForwardedCorrectlyOnSuccess(final String issuedClientRequestId,
															   final Request request,
															   final int expectedCompletionValue) {

		verify(deliveryManager, never()).receiveStatus(Matchers.<RequestStatus>any());

		for (NodeUrn nodeUrn : request.getNodeUrns()) {
			request.getFutureMap().get(nodeUrn).set(null);
		}

		final ArgumentCaptor<RequestStatus> responseCaptor = ArgumentCaptor.forClass(RequestStatus.class);
		verify(deliveryManager, times(request.getNodeUrns().size())).receiveStatus(responseCaptor.capture());

		final List<RequestStatus> capturedStatuses = responseCaptor.getAllValues();

		Set<NodeUrn> receivedStatusNodeUrns = newHashSet();
		for (RequestStatus capturedStatus : capturedStatuses) {

			assertEquals(issuedClientRequestId, capturedStatus.getRequestId());
			assertEquals(1, capturedStatus.getStatus().size());

			for (Status status : capturedStatus.getStatus()) {

				receivedStatusNodeUrns.add(new NodeUrn(status.getNodeId()));

				assertEquals("", status.getMsg());
				assertEquals(expectedCompletionValue, (int) status.getValue());
			}
		}

		assertTrue(Sets.difference(receivedStatusNodeUrns, request.getNodeUrns()).isEmpty());
	}
}
