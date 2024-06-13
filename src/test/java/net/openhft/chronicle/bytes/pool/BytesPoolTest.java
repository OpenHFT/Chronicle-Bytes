package net.openhft.chronicle.bytes.pool;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BytesPoolTest {

    private BytesPool bytesPool;

    @BeforeEach
    void setUp() {
        bytesPool = new BytesPool();
    }

    @Test
    void testAcquireBytes() {
        Bytes<?> bytes = bytesPool.createThreadLocal().get().get();
        assertNotNull(bytes, "Acquired bytes should not be null.");

        assertEquals(0, bytes.readRemaining(), "Acquired bytes should be ready for use.");
    }

    @Test
    void testBytesPoolUsage() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(256);

        bytes.writeUtf8("Hello, World!");
        assertEquals("Hello, World!", bytes.readUtf8());

        bytes.clear();

        assertEquals(0, bytes.readRemaining());

        bytes.releaseLast();
    }
}
