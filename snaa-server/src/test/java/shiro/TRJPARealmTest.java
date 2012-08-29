package shiro;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.Logging;

public class TRJPARealmTest {

	static {
		Logging.setLoggingDefaults();
	}
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(TRJPARealmTest.class);

	private static final String EXPERIMENTER1_PASS = "Pass1";
	private static final String EXPERIMENTER1 = "Experimenter1";
	private static final UsernamePasswordToken experimenter_token = new UsernamePasswordToken(EXPERIMENTER1, EXPERIMENTER1_PASS);

	private static final String ADMINISTRATOR2_PASS = "Pass2";
	private static final String ADMINISTRATOR2 = "Administrator2";
	private static final UsernamePasswordToken administrator_token = new UsernamePasswordToken(ADMINISTRATOR2, ADMINISTRATOR2_PASS);

	@BeforeClass
	public static void setUp() {
		Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory("../localConfigs/shiro.ini");
		org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
		SecurityUtils.setSecurityManager(securityManager);
	}

	@Test
	public void testAuthentication() throws SQLException {

		Subject currentUser = SecurityUtils.getSubject();

		// login the current to check authentication
		currentUser.login(experimenter_token);
		currentUser.logout();

	}
	
	  @Test
	    public void testAuthorizationOKAdmin() throws SQLException {
	        Subject currentUser = SecurityUtils.getSubject();

	        // login the current to check authentication
	        currentUser.login(administrator_token);
	        // check permissions, administrator role has *:* so it is allowed for everything
	        assertTrue(currentUser.isPermittedAll("l:k:j","lk:j:x"));
	        currentUser.logout();
	    }

	@Test
	public void testDoAuthorize() {
		Subject subject = createSubject(ADMINISTRATOR2);
		assertTrue(subject.isPermittedAll("l:k:j","lk:j:x"));
		subject.logout();
	}
	
	@Test
    public void testAuthorizationNOKExperimenter() throws SQLException {
		Subject subject = createSubject(EXPERIMENTER1);
        // check permissions, should not be allowed to flash service nodes
        assertFalse(subject.isPermittedAll("WSN_FLASH_PROGRAMS:SERVICE_NODES"));
		subject.logout();
    }

	// ------------------------------------------------------------------------
	/**
	 * @param userIdentity
	 * @return
	 */
	private Subject createSubject(Object userIdentity) {
		String realmName = "trJPARealm";
		PrincipalCollection principals = new SimplePrincipalCollection(userIdentity, realmName);
		Subject subject = new Subject.Builder().principals(principals).buildSubject();
		return subject;
	}
}
