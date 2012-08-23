package shiro;

import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.tr.util.Logging;

public class JustAJPATest {

    static {
        Logging.setLoggingDefaults();
    }
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JustAJPATest.class);

    @Test
    public void testDoGetAuthenticationInfoAuthenticationToken() {

        log.info("create EntityManager");
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("snaaPersistence");
        EntityManager em =entityManagerFactory.createEntityManager();
       // em.find(entityClass, primaryKey)
    }

    @Test
    public void testDoGetAuthorizationInfoPrincipalCollection() {
        fail("Not yet implemented"); // TODO
    }

}
