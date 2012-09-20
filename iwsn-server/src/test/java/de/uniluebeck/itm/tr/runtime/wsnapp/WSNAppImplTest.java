package de.uniluebeck.itm.tr.runtime.wsnapp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.routing.RoutingTableService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WSNAppImplTest {

	private static final NodeUrn N1 = new NodeUrn("urn:wisebed:uzl1:0x0001");
	private static final NodeUrn N2 = new NodeUrn("urn:wisebed:uzl1:0x0002");
	private static final NodeUrn N3 = new NodeUrn("urn:wisebed:uzl1:0x0003");
	private static final NodeUrn N4 = new NodeUrn("urn:wisebed:uzl1:0x0004");

	private static final NodeUrn M1 = new NodeUrn("urn:wisebed:uzl2:0x0001");
	private static final NodeUrn M2 = new NodeUrn("urn:wisebed:uzl2:0x0002");
	private static final NodeUrn M3 = new NodeUrn("urn:wisebed:uzl2:0x0003");
	private static final NodeUrn M4 = new NodeUrn("urn:wisebed:uzl2:0x0004");

	private static final NodeUrn GW1 = new NodeUrn("urn:wisebed:uzl1:0x1000");
	private static final NodeUrn GW2 = new NodeUrn("urn:wisebed:uzl1:0x1001");
	private static final NodeUrn GW3 = new NodeUrn("urn:wisebed:uzl1:0x1002");

	@Mock
	private WSNAppEventBus wsnAppEventBus;

	@Mock
	private ImmutableSet<String> reservedNodeUrns;

	@Mock
	private TestbedRuntime testbedRuntime;

	@Mock
	private RoutingTableService routingTableService;

	private WSNAppImpl wsnApp;

	@Before
	public void setUp() throws Exception {
		wsnApp = new WSNAppImpl(wsnAppEventBus, testbedRuntime, reservedNodeUrns);

		when(testbedRuntime.getRoutingTableService()).thenReturn(routingTableService);

		when(routingTableService.getNextHop(N1.toString())).thenReturn(GW3.toString());
		when(routingTableService.getNextHop(N2.toString())).thenReturn(GW1.toString());
		when(routingTableService.getNextHop(N3.toString())).thenReturn(GW1.toString());
		when(routingTableService.getNextHop(N4.toString())).thenReturn(GW2.toString());
	}

	@Test
	public void testCalculateGatewayToLinksMap() throws Exception {

		final ImmutableMap<NodeUrn, NodeUrn> links = ImmutableMap.of(
				N1, M1,
				N2, M2,
				N3, M3,
				N4, M4
		);

		final Map<NodeUrn,Map<NodeUrn,NodeUrn>> map = wsnApp.calculateGatewayToLinksMap(links);

		assertEquals(3, map.size());

		assertNotNull(map.get(GW1));
		assertNotNull(map.get(GW2));
		assertNotNull(map.get(GW3));

		assertEquals(M1, map.get(GW3).get(N1));
		assertEquals(M2, map.get(GW1).get(N2));
		assertEquals(M3, map.get(GW1).get(N3));
		assertEquals(M4, map.get(GW2).get(N4));
	}

	@Test
	public void testCalculateGatewayToNodeSet() throws Exception {

		Map<NodeUrn, Set<NodeUrn>> expected = newHashMap();
		expected.put(GW3, newHashSet(N1));
		expected.put(GW1, newHashSet(N2, N3));
		expected.put(GW2, newHashSet(N4));

		final Map<NodeUrn, Set<NodeUrn>> actual = wsnApp.calculateGatewayToNodeSet(newHashSet(N1, N2, N3, N4));

		assertEquals(expected, actual);
	}
}
