package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.AbstractCloseable;
import org.junit.After;
import org.junit.Before;

public class BytesTestCommon {

    @Before
    public void enableCloseableTracing() {
        AbstractCloseable.enableCloseableTracing();
    }

    @After
    public void assertCloseablesClosed() {
        AbstractCloseable.assertCloseablesClosed();
    }

}
