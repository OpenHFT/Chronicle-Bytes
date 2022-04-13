package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ByteCheckSumTest extends BytesTestCommon {
    @Test
    public void test() {
        Bytes<?> bytes = Bytes.allocateDirect(32);
        doTest(bytes);
        bytes.releaseLast();
    }

    @Test
    public void testHeap() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        doTest(bytes);
        bytes.releaseLast();
    }

    private void doTest(Bytes<?> bytes) {
        bytes.append("abcdef");
        assertEquals(('a' + 'b' + 'c' + 'd' + 'e' + 'f') & 0xff, bytes.byteCheckSum());
        assertEquals(('b' + 'c' + 'd' + 'e' + 'f') & 0xff, bytes.byteCheckSum(1, 6));
        assertEquals(('b' + 'c' + 'd') & 0xff, bytes.byteCheckSum(1, 4));
        assertEquals(('c' + 'd') & 0xff, bytes.byteCheckSum(2, 4));
        assertEquals(('c') & 0xff, bytes.byteCheckSum(2, 3));
    }
}
