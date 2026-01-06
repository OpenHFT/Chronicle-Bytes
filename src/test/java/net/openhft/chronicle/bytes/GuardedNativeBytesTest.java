/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GuardedNativeBytes binary primitive and hex dump round trip verification tests.
 */
@SuppressWarnings("deprecation")
@DisplayName("GuardedNativeBytes primitive read and write checks")
public class GuardedNativeBytesTest {

    /**
     * Tests the reading and writing of various binary primitives.
     * <p>
     * This test method performs the following steps:
     * <ul>
     *   <li>Writes different types of binary data into a GuardedNativeBytes object.</li>
     *   <li>Checks the generated hexadecimal string against an expected value.</li>
     *   <li>Reads the binary data back and checks that it matches the original input.</li>
     * </ul>
         */
    @Test
    @DisplayName("binary primitive hex dump round trip")
    public void testBinaryPrimitive() {
        final GuardedNativeBytes<?> guarded = new GuardedNativeBytes<>(BytesStore.nativeStoreWithFixedCapacity(256), 256);
        final HexDumpBytes bytes = new HexDumpBytes(guarded);
        try {
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
            bytes.writeHexDumpDescription("Utf8").writeUtf8("Hello");

            final String expected = "a4 59                                           # flag\n" +
                    "a4 01                                           # s8\n" +
                    "a4 02                                           # u8\n" +
                    "a5 03 00                                        # s16\n" +
                    "a5 04 00                                        # u16\n" +
                    "ae 35                                           # ch\n" +
                    "a5 56 46 a4 9a                                  # s24\n" +
                    "a5 2a 50 a4 fe                                  # u24\n" +
                    "a6 06 00 00 00                                  # s32\n" +
                    "a6 07 00 00 00                                  # u32\n" +
                    "a7 08 00 00 00 00 00 00 00                      # s64\n" +
                    "90 00 00 10 41                                  # f32\n" +
                    "91 00 00 00 00 00 00 24 40                      # f64\n" +
                    "ae 05 48 65 6c 6c 6f                            # Utf8\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual,
                    "Hex dump matches expected output");

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
            final String text = bytes.readUtf8();

            assertTrue(flag, "Boolean flag round trips");
            assertEquals(1, s8, "Signed byte round trips");
            assertEquals(2, u8, "Unsigned byte round trips");
            assertEquals(3, s16, "Signed short round trips");
            assertEquals(4, u16, "Unsigned short round trips");
            assertEquals('5', ch, "Stop bit char round trips");
            assertEquals(-6_666_666, s24, "Signed int24 round trips");
            assertEquals(16_666_666, u24, "Unsigned int24 round trips");
            assertEquals(6, s32, "Signed int round trips");
            assertEquals(7, u32, "Unsigned int round trips");
            assertEquals(8, s64, "Signed long round trips");
            assertEquals(9, f32, 0.0, "Float value round trips correctly");
            assertEquals(10, f64, 0.0, "Double value round trips correctly");
            assertEquals("Hello", text, "UTF8 string round trips");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("GuardedNativeBytes readInt rejects mismatched type markers after writeLong")
    public void readIntRejectsMismatchedType() {
        final GuardedNativeBytes<?> guarded = new GuardedNativeBytes<>(BytesStore.nativeStoreWithFixedCapacity(64), 64);
        try {
            guarded.writeLong(7L);
            assertThrows(IllegalStateException.class,
                    guarded::readInt,
                    "readInt should reject mismatched type markers");
        } finally {
            guarded.releaseLast();
        }
    }

    @Test
    @DisplayName("readStopBit rejects non stop-bit type markers")
    public void readStopBitRejectsMismatchedType() {
        final GuardedNativeBytes<?> guarded = new GuardedNativeBytes<>(BytesStore.nativeStoreWithFixedCapacity(64), 64);
        try {
            guarded.writeInt(5);
            assertThrows(IllegalStateException.class,
                    guarded::readStopBit,
                    "readStopBit should reject non stop-bit type markers");
        } finally {
            guarded.releaseLast();
        }
    }
}
