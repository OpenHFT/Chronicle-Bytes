/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
public class PrimitiveTest extends BytesTestCommon {

    private static final String TEXT_PRIMITIVE_EXPECTED_HEX = "00000000 54 0a 31 0a 32 0a 33 0a  34 2e 31 0a 35 2e 32 0a T·1·2·3· 4.1·5.2·\n" +
            "00000010 2d 49 6e 66 69 6e 69 74  79 0a 36 2e 33 30 30 0a -Infinit y·6.300·\n" +
            "00000020 4e 61 4e 0a                                      NaN·             \n";

    @Test
    public void testBinaryNestedDTO() {
        final Outer outer = new Outer("name", new Inner("key1", 1.1), new Inner("key2", 2.2));

        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            bytes.writeHexDumpDescription("outer");
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

            assertEquals(expected, actual, "nested DTO binary serialization should match expected hex format");

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
            bytes.writeHexDumpDescription("dto");
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

            assertEquals(expected, actual, "primitive DTO binary serialization should match expected hex format");

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
            PrimitiveTestSupport.writeBinaryPrimitivePayload(bytes);

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

            assertEquals(expected, actual, "binary primitive values should match expected hex format");

            // System.out.println(bytes.toHexString());

            PrimitiveTestSupport.assertBinaryPrimitiveValues(bytes, false, null);
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

            assertEquals(expected, actual, "primitives written at explicit offsets should match expected hex format");

            final boolean flag = bytes.readBoolean(0);
            final byte s8 = bytes.readByte(1);
            final int u8 = bytes.readUnsignedByte(2);
            final short s16 = bytes.readShort(3);
            final int u16 = bytes.readUnsignedShort(5);
            final int s32 = bytes.readInt(7);
            final long u32 = bytes.readUnsignedInt(11);
            final long s64 = bytes.readLong(15);
            final float f32 = bytes.readFloat(23);
            final double f64 = bytes.readDouble(27);

            assertTrue(flag, "readBoolean(0) should return true");
            assertEquals(1, s8, "readByte(1) should return written value");
            assertEquals(2, u8, "readUnsignedByte(2) should return written value");
            assertEquals(3, s16, "readShort(3) should return written value");
            assertEquals(4, u16, "readUnsignedShort(5) should return written value");
            assertEquals(6, s32, "readInt(7) should return written value");
            assertEquals(7, u32, "readUnsignedInt(11) should return written value");
            assertEquals(8, s64, "readLong(15) should return written value");
            assertEquals(9, f32, 0.0, "readFloat(23) should return written value");
            assertEquals(10, f64, 0.0, "readDouble(27) should return written value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testTextPrimitiveByteBuffer() {
        assertEquals(TEXT_PRIMITIVE_EXPECTED_HEX, doTestTextPrimitive(Bytes.elasticHeapByteBuffer(64)), "text primitive serialization with ByteBuffer should match expected hex");
    }

    @Test
    public void testTextPrimitiveDirect() {
        assertEquals(TEXT_PRIMITIVE_EXPECTED_HEX, doTestTextPrimitive(Bytes.allocateDirect(64)), "text primitive serialization with direct bytes should match expected hex");
    }

    @Test
    public void testTextPrimitiveHeap() {
        assertEquals(TEXT_PRIMITIVE_EXPECTED_HEX, doTestTextPrimitive(Bytes.allocateElasticOnHeap(64)), "text primitive serialization with heap bytes should match expected hex");
    }

    private String doTestTextPrimitive(Bytes<?> bytes) {
        assumeFalse(NativeBytes.areNewGuarded());
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

            final String actual = bytes.toHexString();

            final boolean flag = bytes.parseBoolean();
            final int s32 = bytes.parseInt();
            final long s64 = bytes.parseLong();
            final String ch = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
            final float f32 = bytes.parseFloat();
            final double f64 = bytes.parseDouble();
            final double f64i = bytes.parseDouble();
            final double f64b = bytes.parseDouble();
            final double f64n = bytes.parseDouble();

            assertTrue(flag, "parseBoolean should return true");
            assertEquals(1, s32, "parseInt should return written value");
            assertEquals(2, s64, "parseLong should return written value");
            assertEquals("3", ch, "parseUtf8 should return character as string");
            assertEquals(4.1, f32, 1e-6, "parseFloat should return written value");
            assertEquals(5.2, f64, 0.0, "parseDouble should return written value");
            assertEquals(Double.NEGATIVE_INFINITY, f64i, 0.5e-4, "parseDouble should handle negative infinity");
            assertEquals(6.2999999, f64b, 0.5e-4, "parseDouble should handle rounded decimal value");
            assertEquals(Double.NaN, f64n, 0.5e-4, "parseDouble should handle NaN");
            return actual;
        } finally {
            bytes.releaseLast();
        }
    }

    static final class Outer implements BytesMarshallable {

        final String name;
        final Inner innerA;
        final Inner innerB;

        Outer(final String name,
              final Inner innerA,
              final Inner innerB) {
            this.name = name;
            this.innerA = innerA;
            this.innerB = innerB;
        }

        Outer() {
            this(null, new Inner(), new Inner());
        }
    }

    public static final class Inner implements BytesMarshallable {

        String key;
        double value;

        Inner(String key, double value) {
            this.key = key;
            this.value = value;
        }

        Inner() {
        }
    }

    static class PrimitiveDTO implements BytesMarshallable {
        boolean flag;
        byte s8;
        short s16;
        char ch;
        int s32;
        long s64;
        float f32;
        double f64;

        PrimitiveDTO(final boolean flag,
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

        PrimitiveDTO() {
        }
    }
}
