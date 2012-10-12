package shiro;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.util.Factory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.snaa.shiro.ShiroSNAA;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.snaa.AuthenticationExceptionException;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SNAAExceptionException;

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
	private static String urnPrefix = "urn:wisebed:uzl2:";

	@BeforeClass
	public static void setUp() {
		String shiroConfigPath = "../localConfigs/shiro.ini";
		Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory(shiroConfigPath);
		org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
		SecurityUtils.setSecurityManager(securityManager);
		Collection<Realm> realms = ((RealmSecurityManager)securityManager).getRealms();
	    if (realms.size() != 1){
	    	throw new RuntimeException("Too many realms configured in "+shiroConfigPath);
	    }
	    realm = realms.iterator().next();
	    
	}

	
	@Test
	public void testAuthentication(){
		// set up Shiro framework
		AuthenticationTriple authTriple = new AuthenticationTriple();
		authTriple.setUsername(EXPERIMENTER1);
		authTriple.setPassword(EXPERIMENTER1_PASS);
		authTriple.setUrnPrefix(urnPrefix);
		List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
		authenticationData.add(authTriple);
    	ShiroSNAA shiroSNAA = new ShiroSNAA(realm,urnPrefix);
    	try {
			shiroSNAA.authenticate(authenticationData);
		} catch (AuthenticationExceptionException e) {
			fail();
		} catch (SNAAExceptionException e) {
			fail();
		}
	}

    @Test
    public void testAuthenticationFailDueToWrongPasswd(){
        // set up Shiro framework
        AuthenticationTriple authTriple = new AuthenticationTriple();
        authTriple.setUsername(EXPERIMENTER1);
        authTriple.setPassword(EXPERIMENTER2_PASS);
        authTriple.setUrnPrefix(urnPrefix);
        List<AuthenticationTriple> authenticationData = new LinkedList<AuthenticationTriple>();
        authenticationData.add(authTriple);
        ShiroSNAA shiroSNAA = new ShiroSNAA(realm,urnPrefix);
        try {
            shiroSNAA.authenticate(authenticationData);
            fail();
        } catch (AuthenticationExceptionException e) {
            // an exception has to be thrown
        } catch (SNAAExceptionException e) {
            // an exception has to be thrown
        }
    }

}
