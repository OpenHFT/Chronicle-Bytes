/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;

public class HexDumpBytesTest extends BytesTestCommon {

    @Test
    public void negativeRandomWritesDoNotResizeOrMutate() {
        // Fixed capacity makes the negative control safe even before validation is fixed.
        HexDumpBytes bytes = new HexDumpBytes(Bytes.allocateDirect(32));
        try {
            bytes.writeLong(123L);
            String dump = bytes.toHexString();
            long position = bytes.writePosition();
            for (Consumer<HexDumpBytes> write : Arrays.<Consumer<HexDumpBytes>>asList(
                    b -> b.writeByte(-1, (byte) 1),
                    b -> b.writeShort(-1, (short) 1),
                    b -> b.writeInt24(-1, 1),
                    b -> b.writeInt(-1, 1),
                    b -> b.writeOrderedInt(-1, 1),
                    b -> b.writeLong(-1, 1),
                    b -> b.writeOrderedLong(-1, 1),
                    b -> b.writeFloat(-1, 1),
                    b -> b.writeDouble(-1, 1))) {
                assertThrows(IllegalArgumentException.class, () -> write.accept(bytes));
                assertEquals(position, bytes.writePosition());
                assertEquals(123L, bytes.readLong(0));
                assertEquals(dump, bytes.toHexString());
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void offsetFormat() {
        doTest(new HexDumpBytes());
    }

    private static void doTest(HexDumpBytes bytes) {
        bytes.numberWrap(8)
        .offsetFormat((o, b) -> b.appendBase16(o, 4));
        bytes.writeHexDumpDescription("hi").write(new byte[18]);
        bytes.adjustHexDumpIndentation(1);
        bytes.writeHexDumpDescription("nest").write(new byte[18]);
        assertEquals("" +
                "0000 00 00 00 00 00 00 00 00 # hi\n" +
                "0008 00 00 00 00 00 00 00 00\n" +
                "0010 00 00\n" +
                "0012    00 00 00 00 00 00 00 00 # nest\n" +
                "001a    00 00 00 00 00 00 00 00\n" +
                "0022    00 00\n", bytes.toHexString());
        bytes.releaseLast();
    }

    @Test
    public void memoryMapped() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File file = new File(OS.getTarget(), "HexDumpBytesTest-" + System.nanoTime() + ".dat");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (MappedBytes mappedBytes = MappedBytes.mappedBytes(file, 64 * 1024)) {
            doTest(new HexDumpBytes(mappedBytes));
        } finally {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }
}
