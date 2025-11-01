/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class BytesUtilTest extends BytesTestCommon {

    @Test
    public void bytesEqualAndCharsEqual() {
        Bytes<?> a = Bytes.from("abcdef");
        Bytes<?> b = Bytes.from("abCdef");
        Bytes<?> c = Bytes.from("abcdef");
        try {
            assertFalse(BytesUtil.bytesEqual(a, 0, b, 0, a.readRemaining()));
            assertTrue(BytesUtil.bytesEqual(a, 0, c, 0, a.readRemaining()));

            assertTrue(BytesUtil.bytesEqual("abc", a, 0, 3));
            assertFalse(BytesUtil.bytesEqual("abC", a, 0, 3));
            assertFalse(BytesUtil.bytesEqual(null, a, 0, 3));
        } finally {
            a.releaseLast();
            b.releaseLast();
            c.releaseLast();
        }
    }

    @Test
    public void asIntStopBitAndPadding() {
        // Validate against native-endian view used by BytesUtil.asInt
        int expected = java.nio.ByteBuffer.wrap("1234".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))
                .order(java.nio.ByteOrder.nativeOrder())
                .getInt();
        assertEquals(expected, BytesUtil.asInt("1234"));
        assertEquals(1, BytesUtil.stopBitLength(0x7F));
        assertEquals(2, BytesUtil.stopBitLength(0x80));
        assertEquals(2, BytesUtil.stopBitLength(0x3FFF));
        assertTrue(BytesUtil.stopBitLength(0x4000) >= 3);

        assertEquals(64L, BytesUtil.roundUpTo64ByteAlign(1));
        assertEquals(0L, BytesUtil.roundUpTo64ByteAlign(0));
        assertEquals(8L, BytesUtil.roundUpTo8ByteAlign(1));
        assertEquals(0L, BytesUtil.padOffset(0));
        assertEquals(2L, BytesUtil.padOffset(2));
    }

    @Test
    public void readWrite8ByteAlignPaddingAndReverseAndCombineNewline() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            bytes.append("hello");
            BytesUtil.read8ByteAlignPadding(bytes);
            assertEquals(0, bytes.readPosition());

            bytes.clear();
            bytes.append("abc");
            long wp = bytes.writePosition();
            BytesUtil.write8ByteAlignPadding(bytes);
            long newWp = bytes.writePosition();
            assertTrue(newWp >= wp);
            for (long i = wp; i < newWp; i++) {
                assertEquals(0, bytes.peekUnsignedByte(i));
            }

            bytes.clear();
            bytes.append("abcdef");
            BytesUtil.reverse(bytes, 0);
            assertEquals("fedcba", bytes.toString());

            bytes.clear();
            bytes.append("line1\n\n");
            BytesUtil.combineDoubleNewline(bytes);
            assertEquals("line1\n", bytes.toString());

            bytes.clear();
            bytes.append("a \n");
            BytesUtil.combineDoubleNewline(bytes);
            assertEquals("a\n", bytes.toString());

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void findAndReadFileLiteral() throws Exception {
        // exercise literal path in readFile
        Bytes<?> literal = BytesUtil.readFile("=XYZ");
        try {
            assertEquals("XYZ", literal.toString());
        } finally {
            literal.releaseLast();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void findFileThrowsWhenMissing() throws Exception {
        BytesUtil.findFile("this-file-should-not-exist-chronicle-bytes");
    }
}
