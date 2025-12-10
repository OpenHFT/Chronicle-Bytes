/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        assertTrue("flag from binary payload", flag);
        assertEquals("s8 from payload", 1, s8);
        assertEquals("u8 from payload", 2, u8);
        assertEquals("s16 from payload", 3, s16);
        assertEquals("u16 from payload", 4, u16);
        assertEquals("char from payload", '5', ch);
        assertEquals("s24 from payload", -6_666_666, s24);
        assertEquals("u24 from payload", 16_666_666, u24);
        assertEquals("s32 from payload", 6, s32);
        assertEquals("u32 from payload", 7, u32);
        assertEquals("s64 from payload", 8, s64);
        assertEquals("f32 from payload", 9, f32, 0.0);
        assertEquals("f64 from payload", 10, f64, 0.0);

        if (expectText) {
            final String text = bytes.readUtf8();
            assertEquals("UTF-8 trailer text mismatch", expectedText, text);
        }
    }
}
