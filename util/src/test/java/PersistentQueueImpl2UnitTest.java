import de.uniluebeck.itm.tr.util.persistentQueue.PersistentQueue;
import de.uniluebeck.itm.tr.util.persistentQueue.impl.PersistentQueueImpl2;
import org.junit.Before;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 11.08.2010
 * Time: 16:26:49
 * To change this template use File | Settings | File Templates.
 */
public class PersistentQueueImpl2UnitTest extends PersistentQueueUnitTest{

    private PersistentQueue queue;
    public PersistentQueueImpl2UnitTest() throws IOException {
        super(new PersistentQueueImpl2("test", 12));
    }

}
