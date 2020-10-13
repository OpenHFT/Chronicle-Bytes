package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class UnsafeRWObjectTest {
    @Test
    public void shortObject() {
        assumeTrue(Jvm.is64bit());
        assertEquals("[12, 32]",
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(AA.class)));
        Bytes bytes = Bytes.allocateDirect(32);
        AA aa = new AA(1, 2, 3);
        bytes.unsafeWriteObject(aa, 12, 4 + 2 * 8);
        assertEquals("" +
                        "00000000 01 00 00 00 02 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                        "00000010 00 00 08 40                                      ···@             \n",
                bytes.toHexString());
        AA a2 = new AA(0, 0, 0);
        bytes.unsafeReadObject(a2, 12, 4 + 2 * 8);
        assertEquals(aa.i, a2.i);
        assertEquals(aa.l, a2.l);
        assertEquals(aa.d, a2.d, 0.0);
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
}
