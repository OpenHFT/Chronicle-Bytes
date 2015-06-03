package net.openhft.chronicle.bytes;

import junit.framework.TestCase;

public class PointerBytesStoreTest extends TestCase {

    public void testWrap() throws Exception {
        NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(10000);

        PointerBytesStore pbs = BytesStore.nativePointer();
        pbs.wrap(nbs.address(), nbs.realCapacity());

        long nanoTime = System.nanoTime();
        pbs.writeLong(0L, nanoTime);

        assertEquals(nanoTime, nbs.readLong(0L));
    }
}