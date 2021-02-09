package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class UnsafeRWObjectTest {
    @Test
    public void shortObject() {
        assumeTrue(Jvm.is64bit() && !Jvm.isAzulZing());
        assertEquals("[12, 32, 32]",
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(AA.class)));
        Bytes bytes = Bytes.allocateDirect(32);
        AA aa = new AA(1, 2, 3);
        bytes.unsafeWriteObject(aa, 4 + 2 * 8);
        assertEquals("" +
                        "00000000 01 00 00 00 02 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                        "00000010 00 00 08 40                                      ···@             \n",
                bytes.toHexString());
        AA a2 = new AA(0, 0, 0);
        bytes.unsafeReadObject(a2, 4 + 2 * 8);
        assertEquals(aa.i, a2.i);
        assertEquals(aa.l, a2.l);
        assertEquals(aa.d, a2.d, 0.0);
        bytes.releaseLast();
    }

    @Test
    public void longObject() {
        assumeTrue(Jvm.is64bit() && !Jvm.isAzulZing());
        assertEquals("[16, 80, 80]",
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(BB.class)));
        Bytes bytes = Bytes.allocateDirect(8 * 8);
        BB bb = new BB(1, 2, 3, 4, 5, 6, 7, 8);
        bytes.unsafeWriteObject(bb, 16, 8 * 8);
        String expected = "" +
                "00000000 01 00 00 00 00 00 00 00  02 00 00 00 00 00 00 00 ········ ········\n" +
                "00000010 03 00 00 00 00 00 00 00  04 00 00 00 00 00 00 00 ········ ········\n" +
                "00000020 05 00 00 00 00 00 00 00  06 00 00 00 00 00 00 00 ········ ········\n" +
                "00000030 07 00 00 00 00 00 00 00  08 00 00 00 00 00 00 00 ········ ········\n";
        assertEquals(expected,
                bytes.toHexString());
        BB b2 = new BB(0, 0, 0, 0, 0, 0, 0, 0);
        bytes.unsafeReadObject(b2, 16, 8 * 8);
        Bytes bytes2 = Bytes.allocateElasticOnHeap(8 * 8);
        bytes2.unsafeWriteObject(b2, 16, 8 * 8);
        assertEquals(expected,
                bytes2.toHexString());

        bytes.releaseLast();
    }

    @Test
    public void array() {
        assumeTrue(Jvm.is64bit() && !Jvm.isAzulZing());
        assertEquals("[16]",
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(byte[].class)));
        Bytes bytes = Bytes.allocateDirect(32);
        byte[] byteArray = "Hello World.".getBytes();
        bytes.unsafeWriteObject(byteArray, byteArray.length);
        assertEquals("" +
                        "00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64 2e             Hello Wo rld.    \n",
                bytes.toHexString());
        byte[] byteArray2 = new byte[byteArray.length];
        bytes.unsafeReadObject(byteArray2, byteArray.length);
        assertEquals("Hello World.", new String(byteArray2));
        bytes.releaseLast();

    }

    static class AA {
        int i;
        long l;
        double d;

        public AA(int i, long l, double d) {
            this.i = i;
            this.l = l;
            this.d = d;
        }
    }

    static class BB {
        long l0, l1, l2, l3, l4, l5, l6, l7;

        public BB(long l0, long l1, long l2, long l3, long l4, long l5, long l6, long l7) {
            this.l0 = l0;
            this.l1 = l1;
            this.l2 = l2;
            this.l3 = l3;
            this.l4 = l4;
            this.l5 = l5;
            this.l6 = l6;
            this.l7 = l7;
        }
    }
}
