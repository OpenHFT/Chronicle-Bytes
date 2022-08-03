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
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.*;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class PrimitiveTest extends BytesTestCommon {

    @Test
    public void testBinaryNestedDTO() {
        final Outer outer = new Outer("name", new Inner("key1", 1.1), new Inner("key2", 2.2));

        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.comment("outer");
            outer.writeMarshallable(bytes);

            final String expected =
                    "                                                # outer\n" +
                            "   04 6e 61 6d 65                                  # name\n" +
                            "                                                # innerA\n" +
                            "      04 6b 65 79 31                                  # key\n" +
                            "      9a 99 99 99 99 99 f1 3f                         # value\n" +
                            "                                                # innerB\n" +
                            "      04 6b 65 79 32                                  # key\n" +
                            "      9a 99 99 99 99 99 01 40                         # value\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            final Outer outer2 = new Outer();
            outer2.readMarshallable(bytes);

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testBinaryPrimitiveDTO() {
        final PrimitiveDTO dto = new PrimitiveDTO(true,
                (byte) 0x11,
                (short) 0x2222,
                '5',
                0x12345678,
                0x123456789ABCDEFL,
                1.2345f,
                Math.PI);

        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.comment("dto");
            dto.writeMarshallable(bytes);

            final String expected = "                                                # dto\n" +
                    "   59                                              # flag\n" +
                    "   11                                              # s8\n" +
                    "   22 22                                           # s16\n" +
                    "   35                                              # ch\n" +
                    "   78 56 34 12                                     # s32\n" +
                    "   ef cd ab 89 67 45 23 01                         # s64\n" +
                    "   19 04 9e 3f                                     # f32\n" +
                    "   18 2d 44 54 fb 21 09 40                         # f64\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            PrimitiveDTO dto2 = new PrimitiveDTO();
            dto2.readMarshallable(bytes);

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testBinaryPrimitive() {
        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.comment("flag").writeBoolean(true);
            bytes.comment("s8").writeByte((byte) 1);
            bytes.comment("u8").writeUnsignedByte(2);
            bytes.comment("s16").writeShort((short) 3);
            bytes.comment("u16").writeUnsignedShort(4);
            bytes.comment("ch").writeStopBit('5');
            bytes.comment("s24").writeInt24(-6_666_666);
            bytes.comment("u24").writeUnsignedInt24(16_666_666);
            bytes.comment("s32").writeInt(6);
            bytes.comment("u32").writeUnsignedInt(7);
            bytes.comment("s64").writeLong(8);
            bytes.comment("f32").writeFloat(9);
            bytes.comment("f64").writeDouble(10);

            final String expected =
                    "59                                              # flag\n" +
                            "01                                              # s8\n" +
                            "02                                              # u8\n" +
                            "03 00                                           # s16\n" +
                            "04 00                                           # u16\n" +
                            "35                                              # ch\n" +
                            "56 46 9a                                        # s24\n" +
                            "2a 50 fe                                        # u24\n" +
                            "06 00 00 00                                     # s32\n" +
                            "07 00 00 00                                     # u32\n" +
                            "08 00 00 00 00 00 00 00                         # s64\n" +
                            "00 00 10 41                                     # f32\n" +
                            "00 00 00 00 00 00 24 40                         # f64\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            // System.out.println(bytes.toHexString());

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
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testBinaryPrimitiveOffset() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        try {

            bytes.writeBoolean(0, true);
            bytes.writeByte(1, (byte) 1);
            bytes.writeUnsignedByte(2, 2);
            bytes.writeShort(3, (short) 3);
            bytes.writeUnsignedShort(5, 4);
            bytes.writeInt(7, 6);
            bytes.writeUnsignedInt(11, 7);
            bytes.writeLong(15, 8);
            bytes.writeFloat(23, 9);
            bytes.writeDouble(27, 10);
            bytes.writePosition(35);

            final String expected =
                    "00000000 59 01 02 03 00 04 00 06  00 00 00 07 00 00 00 08 Y······· ········\n" +
                            "00000010 00 00 00 00 00 00 00 00  00 10 41 00 00 00 00 00 ········ ··A·····\n" +
                            "00000020 00 24 40                                         ·$@              \n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            boolean flag = bytes.readBoolean(0);
            byte s8 = bytes.readByte(1);
            int u8 = bytes.readUnsignedByte(2);
            short s16 = bytes.readShort(3);
            int u16 = bytes.readUnsignedShort(5);
            int s32 = bytes.readInt(7);
            long u32 = bytes.readUnsignedInt(11);
            long s64 = bytes.readLong(15);
            float f32 = bytes.readFloat(23);
            double f64 = bytes.readDouble(27);

            assertTrue(flag);
            assertEquals(1, s8);
            assertEquals(2, u8);
            assertEquals(3, s16);
            assertEquals(4, u16);
            assertEquals(6, s32);
            assertEquals(7, u32);
            assertEquals(8, s64);
            assertEquals(9, f32, 0.0);
            assertEquals(10, f64, 0.0);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testTextPrimitive() {
        assumeFalse(NativeBytes.areNewGuarded());
        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        try {
            bytes.append(true).append('\n');
            bytes.append(1).append('\n');
            bytes.append(2L).append('\n');
            bytes.append('3').append('\n');
            bytes.append(4.1f).append('\n');
            bytes.append(5.2).append('\n');
            bytes.append(Double.NEGATIVE_INFINITY).append('\n');
            bytes.append(6.2999999, 3).append('\n');
            bytes.append(Double.NaN).append('\n');

            final String expected = "00000000 54 0a 31 0a 32 0a 33 0a  34 2e 31 0a 35 2e 32 0a T·1·2·3· 4.1·5.2·\n" +
                    "00000010 2d 49 6e 66 69 6e 69 74  79 0a 36 2e 33 30 30 0a -Infinit y·6.300·\n" +
                    "00000020 4e 61 4e 0a                                      NaN·             \n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual);

            final boolean flag = bytes.parseBoolean();
            final int s32 = bytes.parseInt();
            final long s64 = bytes.parseLong();
            final String ch = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            final float f32 = bytes.parseFloat();
            final double f64 = bytes.parseDouble();
            final double f64i = bytes.parseDouble();
            final double f64b = bytes.parseDouble();
            final double f64n = bytes.parseDouble();

            assertTrue(flag);
            assertEquals(1, s32);
            assertEquals(2, s64);
            assertEquals("3", ch);
            assertEquals(4.1, f32, 1e-6);
            assertEquals(5.2, f64, 0.0);
            assertEquals(Double.NEGATIVE_INFINITY, f64i, 0.5e-4);
            assertEquals(6.2999999, f64b, 0.5e-4);
            assertEquals(Double.NaN, f64n, 0.5e-4);
        } finally {
            bytes.releaseLast();
        }
    }

    private static final class Outer implements BytesMarshallable {

        private final String name;
        private final Inner innerA;
        private final Inner innerB;

        public Outer(final String name,
                     final Inner innerA,
                     final Inner innerB) {
            this.name = name;
            this.innerA = innerA;
            this.innerB = innerB;
        }

        public Outer() {
            this(null, new Inner(), new Inner());
        }
    }

    private static final class Inner implements BytesMarshallable {

        private String key;
        private double value;

        public Inner(String key, double value) {
            this.key = key;
            this.value = value;
        }

        public Inner() {
        }
    }

    private static final class PrimitiveDTO implements BytesMarshallable {
        boolean flag;
        byte s8;
        short s16;
        char ch;
        int s32;
        long s64;
        float f32;
        double f64;

        public PrimitiveDTO(final boolean flag,
                            final byte s8,
                            final short s16,
                            final char ch,
                            final int s32,
                            final long s64,
                            final float f32,
                            final double f64) {
            this.flag = flag;
            this.s8 = s8;
            this.s16 = s16;
            this.ch = ch;
            this.s32 = s32;
            this.s64 = s64;
            this.f32 = f32;
            this.f64 = f64;
        }

        public PrimitiveDTO() {
        }
    }
}
