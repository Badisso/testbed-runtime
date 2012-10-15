package de.uniluebeck.itm.tr.snaa.shiro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.Factory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.jpa.JpaPersistModule;

import de.uniluebeck.itm.tr.snaa.SNAAServer;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.snaa.AuthenticationFault_Exception;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;

public class ShiroSNAATest {

	static {
		Logging.setLoggingDefaults();
	}
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ShiroSNAATest.class);

	private static final String EXPERIMENTER1_PASS = "Pass1";
	private static final String EXPERIMENTER1 = "Experimenter1";
	private static final UsernamePasswordToken experimenter1_token = new UsernamePasswordToken(EXPERIMENTER1, EXPERIMENTER1_PASS);

    private static final String EXPERIMENTER2_PASS = "Pass2";
    private static final String EXPERIMENTER2 = "Experimenter1";
    private static final UsernamePasswordToken experimenter2_token = new UsernamePasswordToken(EXPERIMENTER2, EXPERIMENTER2_PASS);

	private static final String ADMINISTRATOR2_PASS = "Pass2";
	private static final String ADMINISTRATOR2 = "Administrator2";
	private static final UsernamePasswordToken administrator_token = new UsernamePasswordToken(ADMINISTRATOR2, ADMINISTRATOR2_PASS);
	
	private static Realm realm;
	private static NodeUrnPrefix nodeUrnPrefix = new NodeUrnPrefix("urn:wisebed:uzl2:");

	
	@BeforeClass
	public static void setUp() {
		Properties properties = new Properties();
		try {
			properties.load(SNAAServer.class.getClassLoader().getResourceAsStream("META-INF/hibernate.properties"));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		
		Injector injector = Guice.createInjector(new MyShiroModule(), new JpaPersistModule("Default").properties(properties));
		org.apache.shiro.mgt.SecurityManager securityManager = injector.getInstance(org.apache.shiro.mgt.SecurityManager.class);
        
    	SecurityUtils.setSecurityManager(securityManager);
        Collection<Realm> realms = ((RealmSecurityManager)securityManager).getRealms();
        if (realms.size() != 1){
            throw new RuntimeException("Too many realms configured");
        }
        realm = realms.iterator().next();
	    
	}

	
	@Test
	public void testAuthentication(){
		// set up Shiro framework
        List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
    	ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
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
    public void testAuthenticationFailDueToWrongPasswd(){
        // set up Shiro framework
        AuthenticationTriple authTriple = new AuthenticationTriple();
        authTriple.setUsername(EXPERIMENTER1);
        authTriple.setPassword(EXPERIMENTER2_PASS);
        authTriple.setUrnPrefix(nodeUrnPrefix);
        List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
        authenticationData.add(authTriple);
        ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
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
    public void testIsValidWhenValid(){
        List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
        ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            fail();
        }
    }

    @Test
    public void testIsValidWhenUsernameWasChanged(){
        List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
        ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
        List<SecretAuthenticationKey> sakList = null;
        try {
            sakList = shiroSNAA.authenticate(authenticationData);
        } catch (AuthenticationFault_Exception e) {
            fail();
        } catch (SNAAFault_Exception e) {
            fail();
        }
        sakList.get(0).setUsername(ADMINISTRATOR2);
        try {
            assertFalse(shiroSNAA.isValid(sakList.get(0)).isValid());
        } catch (SNAAFault_Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            fail();
        }
    }

    @Test
    public void testIsValidWhenUsernameWasChangedAndIsUnknown(){
        List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
        ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            fail();
        }
    }

    @Test
    public void testIsValidWhenNodeUrnPrefixWasChanged(){
        List<AuthenticationTriple> authenticationData = getAuthenticationTripleListForExperimenter1();
        ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
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
           assertEquals("Not serving node URN prefix urn:wisebed:uzl88:",e.getMessage());
           return;
        }catch (Exception e){
            fail();
        }
    }
    
    @Test
    public void testGetNodeGroupsForNodeURNs(){
    	ShiroSNAA shiroSNAA = new ShiroSNAA(realm, nodeUrnPrefix);
    	shiroSNAA.getNodeGroupsForNodeURNs(Lists.newArrayList(new NodeUrn("urn:wisebed:ulanc1:0x2345")));
    }







    private static List<AuthenticationTriple> getAuthenticationTripleListForExperimenter1() {
        AuthenticationTriple authTriple = new AuthenticationTriple();
        authTriple.setUsername(EXPERIMENTER1);
        authTriple.setPassword(EXPERIMENTER1_PASS);
        authTriple.setUrnPrefix(nodeUrnPrefix);
        List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
        authenticationData.add(authTriple);
        return authenticationData;
    }

    private static List<AuthenticationTriple> getAuthenticationTripleListForExperimenter2() {
        AuthenticationTriple authTriple = new AuthenticationTriple();
        authTriple.setUsername(EXPERIMENTER2);
        authTriple.setPassword(EXPERIMENTER2_PASS);
        authTriple.setUrnPrefix(nodeUrnPrefix);
        List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
        authenticationData.add(authTriple);
        return authenticationData;
    }
}
