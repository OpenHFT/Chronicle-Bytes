/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
@DisplayName("Compare and swap readme example for bytes")
public class CASTest extends BytesTestCommon {
    @Test
    @DisplayName("compare and swap updates int and long")
    public void testCAS() {
        assumeFalse(NativeBytes.areNewGuarded(),
                "Native bytes guards must be disabled for CAS test");

        final HexDumpBytes bytes = new HexDumpBytes()
                .offsetFormat((o, b) -> b.appendBase16(o, 4));
        try {

            bytes.writeHexDumpDescription("s32").writeUtf8("s32");
            bytes.writeSkip((-bytes.writePosition()) & (4 - 1));
            final long s32 = bytes.writePosition();
            assertEquals(0, s32 & 3,
                    "Aligned s32 offset should be word boundary");
            bytes.writeInt(0);

            bytes.writeHexDumpDescription("s64").writeUtf8("s64");
            bytes.writeSkip((-bytes.writePosition()) & (8 - 1));
            final long s64 = bytes.writePosition();
            assertEquals(0, s64 & 7,
                    "Aligned s64 offset should be long boundary");
            bytes.writeLong(0);

            final String expected1 = "0000 03 73 33 32 00 00 00 00                         # s32\n" +
                    "0008 03 73 36 34 00 00 00 00 00 00 00 00 00 00 00 00 # s64\n";

            final String actual1 = bytes.toHexString();

            assertEquals(expected1, actual1,
                    "Initial hex dump matches expected alignment output");


            assertTrue(bytes.compareAndSwapInt(s32, 0, Integer.MAX_VALUE),
                    "CAS int updates value from zero to max");
            assertTrue(bytes.compareAndSwapLong(s64, 0, Long.MAX_VALUE),
                    "CAS long updates value from zero to max");


            final String expected2 = "0000 03 73 33 32 ff ff ff 7f                         # s32\n" +
                    "0008 03 73 36 34 00 00 00 00 ff ff ff ff ff ff ff 7f # s64\n";
            final String actual2 = bytes.toHexString();

            assertEquals(expected2, actual2,
                    "Updated hex dump matches expected CAS output");

        } finally {
            bytes.releaseLast();
        }
    }
}
