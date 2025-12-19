/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class StreamingDataInputTest extends BytesTestCommon {

    public static Object[] params() {
        return Arrays.stream(Allocator.values()).toArray();
    }

    private static void assumeHasNativeMemory(Allocator allocator) {
        assumeFalse(allocator.name().startsWith("NATIVE") && Jvm.maxDirectMemory() == 0);
    }

    @MethodSource("params")
    @ParameterizedTest(name = "allocator={0}")
    public void read(Allocator allocator) {
        assumeHasNativeMemory(allocator);
        Bytes<?> b = allocator.elasticBytes(32);
        b.append("0123456789");
        byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes(ISO_8859_1);
        b.readPosition(3);
        b.read(byteArr);
        assertEquals("3456789HIJKLMNOP", new String(byteArr, ISO_8859_1), "read() should start at readPosition(3) and fill entire byte array");
        b.releaseLast();
    }

    @MethodSource("params")
    @ParameterizedTest(name = "allocator={0}")
    public void readOffset(Allocator allocator) {
        assumeHasNativeMemory(allocator);
        Bytes<?> b = allocator.elasticBytes(32);
        b.append("0123456789");
        byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes(ISO_8859_1);
        b.read(byteArr, 2, 6);
        assertEquals("AB012345IJKLMNOP", new String(byteArr, ISO_8859_1), "read(array, 2, 6) should write 6 bytes starting at array offset 2");
        assertEquals('6', b.readByte(), "Next readByte should return character '6' after reading 6 bytes");
        b.releaseLast();
    }

    @MethodSource("params")
    @ParameterizedTest(name = "allocator={0}")
    public void roundTripWorksOnHeap(Allocator allocator) {
        assumeHasNativeMemory(allocator);
        Bytes<?> b = allocator.elasticBytes(32);
        SampleObject source = new SampleObject(123L, 123, false);
        int offset = BytesUtil.triviallyCopyableStart(source.getClass());
        b.unsafeWriteObject(source, offset, 13);
        SampleObject dest = new SampleObject();
        b.unsafeReadObject(dest, offset, 13);
        assertEquals(source, dest, "unsafeWriteObject/unsafeReadObject round-trip should preserve object field values");
        b.releaseLast();
    }

    @MethodSource("params")
    @ParameterizedTest(name = "allocator={0}")
    public void readWithLength(Allocator allocator) {
        assumeHasNativeMemory(allocator);
        int max = 130; // two bytes of length for a stop bit encoded length
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(max + 2);
        Bytes<?> from = Bytes.wrapForRead(new byte[max]);
        Bytes<?> to = Bytes.wrapForRead(new byte[max]);
        for (int len = 0; len <= max; len++) {
            from.readPositionRemaining(0, len);
            bytes.clear();
            bytes.writeWithLength(from);
            assertEquals(len + (len < 128 ? 1 : 2), bytes.readRemaining(), "readRemaining should include length encoding (1 or 2 bytes) plus data");
            bytes.readWithLength(to);
            assertEquals(len, to.readRemaining(), "Target bytes should contain exact data length after readWithLength");
        }
    }

    static class SampleObject {
        long l1;
        long i1;
        boolean b1;

        SampleObject() {
        }

        SampleObject(long l1, int i1, boolean b1) {
            this.l1 = l1;
            this.i1 = i1;
            this.b1 = b1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SampleObject that = (SampleObject) o;
            return l1 == that.l1 && i1 == that.i1 && b1 == that.b1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l1, i1, b1);
        }

        @Override
        public String toString() {
            return "SampleObject{" +
                    "l1=" + l1 +
                    ", i1=" + i1 +
                    ", b1=" + b1 +
                    '}';
        }
    }
}
