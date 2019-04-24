package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class ReadLenientTest {
    @Test
    public void testLenient() {
        assumeFalse(NativeBytes.areNewGuarded());
        doTest(Bytes.allocateDirect(64));
        doTest(Bytes.elasticHeapByteBuffer(64));
        doTest(Bytes.from(""));
    }

    @SuppressWarnings("rawtypes")
    private void doTest(Bytes bytes) {
        bytes.lenient(true);
        ByteBuffer bb = ByteBuffer.allocateDirect(32);
        bytes.read(bb);
        assertEquals(0, bb.position());

        assertEquals(BigDecimal.ZERO, bytes.readBigDecimal());
        assertEquals(BigInteger.ZERO, bytes.readBigInteger());
        assertEquals(false, bytes.readBoolean());
        assertEquals(null, bytes.read8bit());
        assertEquals(null, bytes.readUtf8());
        assertEquals(0, bytes.readByte());
        assertEquals(-1, bytes.readUnsignedByte()); // note this behaviour is need to find the end of a stream.
        assertEquals(0, bytes.readShort());
        assertEquals(0, bytes.readUnsignedShort());
        assertEquals(0, bytes.readInt());
        assertEquals(0, bytes.readUnsignedInt());
        assertEquals(0.0, bytes.readFloat(), 0.0);
        assertEquals(0.0, bytes.readDouble(), 0.0);
        bytes.readSkip(8);
        assertEquals(0, bytes.readPosition());

        bytes.release();
    }
}
