/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class contains JUnit test methods for testing the behavior
 * of the GuardedNativeBytes class.
 * <p>
 * It aims to test various primitive data types and their conversions
 * using the GuardedNativeBytes class.
 * </p>
 */
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
     * </p>
     */
    @Test
    public void testBinaryPrimitive() {
        final GuardedNativeBytes<?> bytes = new GuardedNativeBytes(new HexDumpBytes(), 256);
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

            final String expected = "" +
                    "a4 59                                           # flag\n" +
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

            assertEquals(expected, actual);

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

            assertTrue(flag);
            assertEquals(1, s8);
            assertEquals(2, u8);
            assertEquals(3, s16);
            assertEquals(4, u16);
            assertEquals('5', ch);
            assertEquals(-6_666_666, s24);
            assertEquals(16_666_666, u24);
            assertEquals(6, s32);
            assertEquals(7, u32);
            assertEquals(8, s64);
            assertEquals(9, f32, 0.0);
            assertEquals(10, f64, 0.0);
            assertEquals("Hello", text);
        } finally {
            bytes.releaseLast();
        }
    }
}