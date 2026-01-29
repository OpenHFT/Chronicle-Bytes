/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test support utilities for primitive binary operations because shared
 * assertions help maintain consistent validation across multiple primitive
 * read/write tests.
 */
@SuppressWarnings({"deprecation", "MMOverusedWord"}) // payload domain terminology
public final class PrimitiveTestSupport {

    private PrimitiveTestSupport() {
    }

    public static void writeBinaryPrimitivePayload(Bytes<?> bytes) {
        bytes.writeHexDumpDescription("flag").writeBoolean(true);
        bytes.writeHexDumpDescription("s8").writeByte((byte) 1);
        bytes.writeHexDumpDescription("u8").writeUnsignedByte(2);
        bytes.writeHexDumpDescription("s16").writeShort((short) 3);
        bytes.writeHexDumpDescription("u16").writeUnsignedShort(4);
        bytes.writeHexDumpDescription("ch").writeStopBit('5');
        bytes.writeHexDumpDescription("s24").writeInt24(-6_666_666);
        bytes.writeHexDumpDescription("u24").writeUnsignedInt24(16_666_666);
        bytes.writeHexDumpDescription("s32").writeInt(6);
        bytes.writeHexDumpDescription("u32").writeUnsignedInt(7);
        bytes.writeHexDumpDescription("s64").writeLong(8);
        bytes.writeHexDumpDescription("f32").writeFloat(9);
        bytes.writeHexDumpDescription("f64").writeDouble(10);
    }

    public static void assertBinaryPrimitiveValues(Bytes<?> bytes, boolean expectText, String expectedText) {
        final boolean flag = bytes.readBoolean();
        final byte s8 = bytes.readByte();
        final int u8 = bytes.readUnsignedByte();
        final short s16 = bytes.readShort();
        final int u16 = bytes.readUnsignedShort();
        final char ch = bytes.readStopBitChar();
        final int s24 = bytes.readInt24();
        final long u24 = bytes.readUnsignedInt24();
        final int s32 = bytes.readInt();
        final long u32 = bytes.readUnsignedInt();
        final long s64 = bytes.readLong();
        final float f32 = bytes.readFloat();
        final double f64 = bytes.readDouble();

        assertTrue(flag, "Boolean flag from binary payload should be true");
        assertEquals(1, s8, "Signed byte s8 from payload should equal one");
        assertEquals(2, u8, "Unsigned byte u8 from payload should equal two");
        assertEquals(3, s16, "Signed short s16 from payload should equal three");
        assertEquals(4, u16, "Unsigned short u16 from payload should equal four");
        assertEquals('5', ch, "Char ch from payload should equal digit five");
        assertEquals(-6_666_666, s24, "Signed int24 s24 from payload should match");
        assertEquals(16_666_666, u24, "Unsigned int24 u24 from payload should match");
        assertEquals(6, s32, "Signed int s32 from payload should equal six");
        assertEquals(7, u32, "Unsigned int u32 from payload should equal seven");
        assertEquals(8, s64, "Signed long s64 from payload should equal eight");
        assertEquals(9, f32, 0.0, "Float f32 from payload should equal nine");
        assertEquals(10, f64, 0.0, "Double f64 from payload should equal ten");

        if (expectText) {
            final String text = bytes.readUtf8();
            assertEquals(expectedText, text, "read UTF-8 trailer text should match expected value");
        }
    }
}
