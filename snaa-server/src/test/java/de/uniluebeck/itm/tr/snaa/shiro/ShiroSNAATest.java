package de.uniluebeck.itm.tr.snaa.shiro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

import de.uniluebeck.itm.tr.snaa.SNAAServer;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.common.UsernameUrnPrefixPair;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthenticationFault_Exception;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;

public class ShiroSNAATest {

	static {
		Logging.setLoggingDefaults();
	}
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ShiroSNAATest.class);

	private static final String EXPERIMENTER1_PASS = "Exp1Pass";
	private static final String EXPERIMENTER1 = "Experimenter1";
	private static final UsernamePasswordToken experimenter1_token = new UsernamePasswordToken(EXPERIMENTER1, EXPERIMENTER1_PASS);

	private static final String EXPERIMENTER2_PASS = "Exp2Pass";
	private static final String EXPERIMENTER2 = "Experimenter2";
	private static final UsernamePasswordToken experimenter2_token = new UsernamePasswordToken(EXPERIMENTER2, EXPERIMENTER2_PASS);

	private static final String SERVICE_PROVIDER1_PASS = "SP1Pass";
	private static final String SERVICE_PROVIDER1 = "ServiceProvider1";
	private static final UsernamePasswordToken SERVICE_PROVIDER1_TOKEN = new UsernamePasswordToken(SERVICE_PROVIDER1, SERVICE_PROVIDER1_PASS);

	private static final String ADMINISTRATOR1_PASS = "Adm1Pass";
	private static final String ADMINISTRATOR1 = "Administrator1";
	private static final UsernamePasswordToken administrator_token = new UsernamePasswordToken(ADMINISTRATOR1, ADMINISTRATOR1_PASS);

	private NodeUrnPrefix nodeUrnPrefix = new NodeUrnPrefix("urn:wisebed:uzl2:");

	private ShiroSNAA shiroSNAA;

	@Before
	public void setUp() {
		Properties properties = new Properties();
		try {
			properties.load(SNAAServer.class.getClassLoader().getResourceAsStream("META-INF/hibernate.properties"));
		} catch (IOException e) {
			log.error(e.getMessage(), e);

		}
		Injector jpaInjector = Guice.createInjector(new JpaPersistModule("Default").properties(properties));
		jpaInjector.getInstance(PersistService.class).start();

		MyShiroModule myShiroModule = new MyShiroModule();
		Injector shiroInjector = jpaInjector.createChildInjector(myShiroModule);
		SecurityUtils.setSecurityManager(shiroInjector.getInstance(org.apache.shiro.mgt.SecurityManager.class));

		ShiroSNAAFactory factory = shiroInjector.getInstance(ShiroSNAAFactory.class);
		shiroSNAA = factory.create(nodeUrnPrefix);
	}

	@Test
	public void testAuthenticationForExperimenter1() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		try {
			List<SecretAuthenticationKey> sakList = shiroSNAA.authenticate(authenticationData);
			assertNotNull(sakList);
			assertEquals(EXPERIMENTER1, sakList.get(0).getUsername());
			assertEquals(nodeUrnPrefix, sakList.get(0).getUrnPrefix());
			assertNotNull(sakList.get(0).getSecretAuthenticationKey());
		} catch (AuthenticationFault_Exception e) {
			fail();
		} catch (SNAAFault_Exception e) {
			fail();
		}

	}

	@Test
	public void testAuthenticationFailForExperimenter1DueToWrongPasswd() {

		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER2_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		try {
			shiroSNAA.authenticate(authenticationData);
			fail();
		} catch (AuthenticationFault_Exception e) {
			// an exception has to be thrown
		} catch (SNAAFault_Exception e) {
			// an exception has to be thrown
		}
	}

	@Test
	public void testAuthenticationForServiceProvider1() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForServiceProvider1();
		try {
			List<SecretAuthenticationKey> sakList = shiroSNAA.authenticate(authenticationData);
			assertNotNull(sakList);
			assertEquals(SERVICE_PROVIDER1, sakList.get(0).getUsername());
			assertEquals(nodeUrnPrefix, sakList.get(0).getUrnPrefix());
			assertNotNull(sakList.get(0).getSecretAuthenticationKey());
		} catch (AuthenticationFault_Exception e) {
			fail();
		} catch (SNAAFault_Exception e) {
			fail();
		}

	}

	@Test
	public void testIsValidWhenValid() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			fail();
		} catch (SNAAFault_Exception e) {
			fail();
		}

		try {
			assertTrue(shiroSNAA.isValid(sakList.get(0)).isValid());
		} catch (SNAAFault_Exception e) {
			e.printStackTrace(); // To change body of catch statement use File | Settings | File
									// Templates.
			fail();
		}
	}

	@Test
	public void testIsValidWhenUsernameWasChanged() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			fail();
		} catch (SNAAFault_Exception e) {
			fail();
		}
		sakList.get(0).setUsername(ADMINISTRATOR1);
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
		} catch (SNAAFault_Exception e) {
			e.printStackTrace(); // To change body of catch statement use File | Settings | File
									// Templates.
			fail();
		}
	}

	@Test
	public void testIsValidWhenUsernameWasChangedAndIsUnknown() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			fail();
		} catch (SNAAFault_Exception e) {
			fail();
		}
		sakList.get(0).setUsername("Trudy");
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
		} catch (SNAAFault_Exception e) {
			e.printStackTrace(); 
			fail();
		}
	}

	@Test
	public void testIsValidWhenNodeUrnPrefixWasChanged() {

		List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
		List<SecretAuthenticationKey> sakList = null;
		try {
			sakList = shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationFault_Exception e) {
			fail();
		} catch (SNAAFault_Exception e) {
			fail();
		}
		sakList.get(0).setUrnPrefix(new NodeUrnPrefix("urn:wisebed:uzl88:"));
		try {
			assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
			return;
		} catch (SNAAFault_Exception e) {
			// expected exception if one is thrown:
			assertEquals("Not serving node URN prefix urn:wisebed:uzl88:", e.getMessage());
			return;
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void testGetNodeGroupsForNodeURNsForSingleURN() {
		Set<String> nodeGroupsForNodeURNs = shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211")));
		assertTrue(nodeGroupsForNodeURNs.size() == 1);
		assertEquals("EXPERIMENT_NODES", nodeGroupsForNodeURNs.iterator().next());
	}
	
	@Test
	public void testGetNodeGroupsForNodeURNsForMultipleURNs() {
		Set<String> nodeGroupsForNodeURNs = shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:uzl2:0x2211"),new NodeUrn("urn:wisebed:uzl2:0x2311")));
		assertTrue(nodeGroupsForNodeURNs.size() == 2);
		SortedSet<String> actual = new TreeSet<String>(nodeGroupsForNodeURNs);
		SortedSet<String> expected = new TreeSet<String>(Lists.newArrayList("EXPERIMENT_NODES","SERVICE_NODES"));
		assertEquals(expected,actual);
	}

	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1, nodeUrnPrefix, "urn:wisebed:uzl2:0x2211");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			fail();
		}
	}
	
	@Test
	public void testIsAuthorizedForAdministrator1OnExperimentNodeFromWrongNetwork() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(ADMINISTRATOR1, nodeUrnPrefix, "urn:wisebed:ulanc:0x1211");
		try {
			shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized();
			fail();
		} catch (SNAAFault_Exception e) {
			// expected exception 
			assertEquals("Not serving node URN prefix 'urn:wisebed:ulanc:'", e.getMessage());
		}
	}

	@Test
	public void testIsAuthorizedForExperimenter1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(EXPERIMENTER1, nodeUrnPrefix, "urn:wisebed:uzl2:0x2211");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.SM_ARE_NODES_ALIVE).isAuthorized());
		} catch (SNAAFault_Exception e) {
			fail();
		}
	}

	@Test
	public void testIsAuthorizedForServiceProvider1OnExperimentNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefix, "urn:wisebed:uzl2:0x2211");
		try {
			assertFalse(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			fail();
		}
	}
	
	@Test
	public void testIsAuthorizedForServiceProvider1OnServiceNode() {
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = createUsernameNodeUrnsMapList(SERVICE_PROVIDER1, nodeUrnPrefix, "urn:wisebed:uzl2:0x2311");
		try {
			assertTrue(shiroSNAA.isAuthorized(usernameNodeUrnsMaps, Action.WSN_FLASH_PROGRAMS).isAuthorized());
		} catch (SNAAFault_Exception e) {
			fail();
		}
	}
	
	
	
	
	/* ------------------------------ Helpers ----------------------------------- */

	private List<UsernameNodeUrnsMap> createUsernameNodeUrnsMapList(String username, NodeUrnPrefix nodeUrnPrefix, String... nodeURNStrings){
		List<UsernameNodeUrnsMap> usernameNodeUrnsMaps = new LinkedList<UsernameNodeUrnsMap>();
		UsernameNodeUrnsMap map = new UsernameNodeUrnsMap();
		UsernameUrnPrefixPair usernameUrnPrefixPair = new UsernameUrnPrefixPair();
		usernameUrnPrefixPair.setUrnPrefix(nodeUrnPrefix);
		usernameUrnPrefixPair.setUsername(username);
		map.setUsername(usernameUrnPrefixPair);
		List<NodeUrn> nodeUrns = map.getNodeUrns();
		
		for (String nodeUrnString : nodeURNStrings) {
			nodeUrns.add(new NodeUrn(nodeUrnString));
		}

		usernameNodeUrnsMaps.add(map);
		return usernameNodeUrnsMaps;
	}
	
	
	private List<AuthenticationTriple> getAuthenticationTripleListForExperimenter1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}

	private List<AuthenticationTriple> getAuthenticationTripleListForExperimenter2() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER2);
		authTriple.setPassword(EXPERIMENTER2_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}
	
	private List<AuthenticationTriple> getAuthenticationTripleListForServiceProvider1() {
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(SERVICE_PROVIDER1);
		authTriple.setPassword(SERVICE_PROVIDER1_PASS);
		authTriple.setUrnPrefix(nodeUrnPrefix);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
		return authenticationData;
	}
}
