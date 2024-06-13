package net.openhft.chronicle.bytes;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ByteableTest {

    private Byteable byteable;
    private BytesStore<?, ?> bytesStore;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException {
        byteable = mock(Byteable.class);
        bytesStore = mock(BytesStore.class);
        doThrow(UnsupportedOperationException.class).when(byteable).address();
        doThrow(UnsupportedOperationException.class).when(byteable).lock(true);
        doThrow(UnsupportedOperationException.class).when(byteable).tryLock(true);
    }

    @Test
    public void testOffset() {
        long expectedOffset = 5L;
        when(byteable.offset()).thenReturn(expectedOffset);

        assertEquals(expectedOffset, byteable.offset());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddressThrowsUnsupportedOperationException() {
        when(byteable.address()).thenCallRealMethod();
        byteable.address();
    }

    @Test
    public void testMaxSize() {
        long expectedSize = 1024L;
        when(byteable.maxSize()).thenReturn(expectedSize);

        assertEquals(expectedSize, byteable.maxSize());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLockThrowsUnsupportedOperationException() throws IOException {
        byteable.lock(true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTryLockThrowsUnsupportedOperationException() throws IOException {
        byteable.tryLock(true);
    }
}
