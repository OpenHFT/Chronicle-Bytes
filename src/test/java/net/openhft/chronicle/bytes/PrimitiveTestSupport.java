/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
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

        assertTrue(flag, "flag from binary payload");
        assertEquals(1, s8, "s8 from payload");
        assertEquals(2, u8, "u8 from payload");
        assertEquals(3, s16, "s16 from payload");
        assertEquals(4, u16, "u16 from payload");
        assertEquals('5', ch, "char from payload");
        assertEquals(-6_666_666, s24, "s24 from payload");
        assertEquals(16_666_666, u24, "u24 from payload");
        assertEquals(6, s32, "s32 from payload");
        assertEquals(7, u32, "u32 from payload");
        assertEquals(8, s64, "s64 from payload");
        assertEquals(9, f32, 0.0, "f32 from payload");
        assertEquals(10, f64, 0.0, "f64 from payload");

        if (expectText) {
            final String text = bytes.readUtf8();
            assertEquals(expectedText, text, "read UTF-8 trailer text should match expected value");
        }
    }
}
