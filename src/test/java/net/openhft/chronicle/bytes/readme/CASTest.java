package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class CASTest {
    @Test
    public void testCAS() {
        assumeFalse(NativeBytes.areNewGuarded());

        HexDumpBytes bytes = new HexDumpBytes()
                .offsetFormat((o, b) -> b.appendBase16(o, 4));

        bytes.comment("s32").writeUtf8("s32");
        bytes.alignBy(4);
        long s32 = bytes.writePosition();
        bytes.writeInt(0);

        bytes.comment("s64").writeUtf8("s64");
        bytes.alignBy(8);
        long s64 = bytes.writePosition();
        bytes.writeLong(0);

        System.out.println(bytes.toHexString());

        assertTrue(bytes.compareAndSwapInt(s32, 0, Integer.MAX_VALUE));
        assertTrue(bytes.compareAndSwapLong(s64, 0, Long.MAX_VALUE));

        System.out.println(bytes.toHexString());

        assertEquals("0000 03 73 33 32 ff ff ff 7f                         # s32\n" +
                        "0008 03 73 36 34 00 00 00 00 ff ff ff ff ff ff ff 7f # s64\n",
                bytes.toHexString());
        bytes.release();
    }
}
