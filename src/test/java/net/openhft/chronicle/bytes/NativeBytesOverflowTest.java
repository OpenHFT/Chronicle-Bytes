package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.Assert.assertTrue;

public class NativeBytesOverflowTest extends BytesTestCommon {

    @Test(expected = BufferOverflowException.class)
    public void testExceedWriteLimitNativeWriteBytes() {
        BytesStore<?, ByteBuffer> store = wrap(ByteBuffer.allocate(128));
        Bytes nb = new NativeBytes(store);
        try {
            nb.writeLimit(2).writePosition(0);
            nb.writeLong(10L);
        } finally {
            nb.releaseLast();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testExceedWriteLimitGuardedBytes() {
        Bytes guardedNativeBytes = new GuardedNativeBytes(wrap(ByteBuffer.allocate(128)), 128);
        try {
            guardedNativeBytes.writeLimit(2).writePosition(0);
            guardedNativeBytes.writeLong(10L);
        } finally {
            guardedNativeBytes.releaseLast();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testElastic() {
        Bytes bytes = Bytes.elasticByteBuffer();
        try {
            bytes.writeLimit(2).writePosition(0);
            bytes.writeLong(10L);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testNativeWriteBytes2() {
        Bytes nb = new NativeBytes(wrap(ByteBuffer.allocate(128))).unchecked(true);

        nb.writeLimit(2).writePosition(0);
        nb.writeLong(10L);

        // this is OK as we are unchecked !
        assertTrue(nb.writePosition() > nb.writeLimit());
    }
}
