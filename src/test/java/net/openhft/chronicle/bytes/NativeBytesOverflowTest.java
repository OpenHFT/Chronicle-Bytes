package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.wrap;

public class NativeBytesOverflowTest {

    @Test(expected = BufferOverflowException.class)
    public void testExceedWriteLimitNativeWriteBytes() {
        BytesStore<?, ByteBuffer> store = wrap(ByteBuffer.allocate(128));
        Bytes nb = new NativeBytes(store, 128);

        nb.writeLimit(2).writePosition(0);
        nb.writeLong(10L);

        // should never get here
        Assert.fail();
    }

    @Test(expected = BufferOverflowException.class)
    public void testExceedWriteLimitGuardedBytes() {
        Bytes guardedNativeBytes = new GuardedNativeBytes(wrap(ByteBuffer.allocate(128)), 128);
        guardedNativeBytes.writeLimit(2).writePosition(0);
        guardedNativeBytes.writeLong(10L);

        // should never get here
        Assert.fail();
    }

    @Test(expected = BufferOverflowException.class)
    public void testElastic() {
        Bytes bytes = Bytes.elasticByteBuffer();
        bytes.writeLimit(2).writePosition(0);
        bytes.writeLong(10L);

        // should never get here
        Assert.fail();
    }

    @Test
    public void testNativeWriteBytes2() {
        Bytes nb = new NativeBytes(wrap(ByteBuffer.allocate(128)), 128).unchecked(true);

        nb.writeLimit(2).writePosition(0);
        nb.writeLong(10L);

        // this is OK as we are unchecked !
    }
}
