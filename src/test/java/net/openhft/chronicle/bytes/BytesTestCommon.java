package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.After;
import org.junit.Before;

public class BytesTestCommon {

    @Before
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    @After
    public void assertReferencesReleased() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

}