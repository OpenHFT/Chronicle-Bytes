/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.MappedBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class StringInternerBytesTest extends BytesTestCommon {

    @Test
    public void testIntern() {
        @NotNull StringInternerBytes si = new StringInternerBytes(128);
        for (int i = 0; i < 100; i++) {
            Bytes<?> b = Bytes.from("key" + i);
            si.intern(b, (int) b.readRemaining());
            b.releaseLast();
        }
        assertEquals(89, si.valueCount());
    }

    private static Bytes<?> allocate(boolean direct) {
        return direct ? Bytes.allocateElasticDirect(512) : Bytes.allocateElasticOnHeap(512);
    }

    @Test
    public void missMapsEveryByteValueToTheSameChar() {
        for (boolean direct : new boolean[]{false, true}) {
            StringInternerBytes si = new StringInternerBytes(64);
            Bytes<?> b = allocate(direct);
            try {
                StringBuilder expected = new StringBuilder();
                for (int i = 0; i < 256; i++) {
                    b.writeUnsignedByte(i);
                    expected.append((char) i);
                }
                String first = si.intern(b, 256);
                assertEquals(expected.toString(), first);
                assertEquals(0, b.readRemaining());

                // Looking the same bytes up again must give an equal String. Whether it is the pooled
                // instance depends on BytesStore.isEqual, which is outside this class.
                b.readPosition(0);
                assertEquals(first, si.intern(b, 256));
            } finally {
                b.releaseLast();
            }
        }
    }

    @Test
    public void missMatchesCharArrayConversionForEveryLength() {
        Random random = new Random(1);
        for (boolean direct : new boolean[]{false, true}) {
            StringInternerBytes si = new StringInternerBytes(256);
            Bytes<?> b = allocate(direct);
            try {
                for (int length = 0; length <= 300; length++) {
                    b.clear();
                    b.write("pad".getBytes(ISO_8859_1));
                    for (int i = 0; i < length; i++)
                        b.writeUnsignedByte(random.nextInt(256));
                    b.write("trail".getBytes(ISO_8859_1));
                    b.readPosition(3);
                    String expected = new String(BytesUtil.toCharArray(b, 3, length));

                    String actual = si.intern(b, length);

                    assertEquals("length " + length, expected, actual);
                    assertEquals("only the interned bytes are consumed", 3L + length, b.readPosition());
                }
            } finally {
                b.releaseLast();
            }
        }
    }

    @Test
    public void emptyValueIsInternedWithoutConsumingInput() {
        StringInternerBytes si = new StringInternerBytes(16);
        Bytes<?> b = Bytes.from("abc");
        try {
            assertEquals("", si.intern(b, 0));
            assertEquals("", si.intern(b, 0));
            assertEquals(3, b.readRemaining());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void internedStringDoesNotAliasTheSourceBuffer() {
        for (boolean direct : new boolean[]{false, true}) {
            StringInternerBytes si = new StringInternerBytes(16);
            Bytes<?> b = allocate(direct);
            try {
                b.write("ORDER-1".getBytes(ISO_8859_1));
                String id = si.intern(b, 7);
                b.clear();
                b.write("XXXXXXX".getBytes(ISO_8859_1));
                assertEquals("ORDER-1", id);
            } finally {
                b.releaseLast();
            }
        }
    }

    @Test
    public void uniqueValuesAreDistinctStringsAndRepeatsArePooled() {
        StringInternerBytes si = new StringInternerBytes(1024);
        Bytes<?> b = Bytes.allocateElasticDirect(64);
        try {
            b.write("ORD-0000001".getBytes(ISO_8859_1));
            String one = si.intern(b, 11);
            b.clear().write("ORD-0000002".getBytes(ISO_8859_1));
            String two = si.intern(b, 11);
            b.clear().write("ORD-0000001".getBytes(ISO_8859_1));
            String oneAgain = si.intern(b, 11);

            assertEquals("ORD-0000001", one);
            assertEquals("ORD-0000002", two);
            assertNotSame(one, two);
            assertSame(one, oneAgain);
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void internsFromMappedBytes() throws IOException {
        File file = File.createTempFile("string-interner-bytes", ".dat");
        file.deleteOnExit();
        StringInternerBytes si = new StringInternerBytes(16);
        MappedBytes b = MappedBytes.mappedBytes(file, 64 << 10);
        try {
            b.write("MAPPED-ID-42".getBytes(ISO_8859_1));
            String id = si.intern(b, 12);
            assertEquals("MAPPED-ID-42", id);
            b.readPosition(0);
            assertSame(id, si.intern(b, 12));
        } finally {
            b.releaseLast();
        }
    }
}
