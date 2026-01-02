/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class StreamingDataInputTest extends BytesTestCommon {

    public static Stream<Allocator> params() {
        return Arrays.stream(Allocator.values());
    }

    private static void assumeNativeMemory(Allocator allocator) {
        assumeFalse(allocator.name().startsWith("NATIVE") && Jvm.maxDirectMemory() == 0,
                "Native allocators require direct memory");
    }

    @ParameterizedTest(name = "allocator {0} reads bytes from position")
    @DisplayName("read fills array from the current read position")
    @MethodSource("params")
    public void read(Allocator allocator) {
        assumeNativeMemory(allocator);
        Bytes<?> b = allocator.elasticBytes(32);
        try {
            b.append("0123456789");
            byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes(StandardCharsets.ISO_8859_1);
            b.readPosition(3);
            b.read(byteArr);
            assertEquals("3456789HIJKLMNOP",
                    new String(byteArr, StandardCharsets.ISO_8859_1),
                    "Read should overwrite the array with remaining bytes");
        } finally {
            b.releaseLast();
        }
    }

    @ParameterizedTest(name = "allocator {0} reads bytes into slice")
    @DisplayName("read with offset updates array and advances position")
    @MethodSource("params")
    public void readOffset(Allocator allocator) {
        assumeNativeMemory(allocator);
        Bytes<?> b = allocator.elasticBytes(32);
        try {
            b.append("0123456789");
            byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes(StandardCharsets.ISO_8859_1);
            b.read(byteArr, 2, 6);
            assertEquals("AB012345IJKLMNOP",
                    new String(byteArr, StandardCharsets.ISO_8859_1),
                    "Offset read should update only the target slice");
            assertEquals('6',
                    b.readByte(),
                    "Read position should advance past the copied bytes");
        } finally {
            b.releaseLast();
        }
    }

    @ParameterizedTest(name = "allocator {0} round trips unsafe copy")
    @DisplayName("unsafe copy round trip preserves object state")
    @MethodSource("params")
    public void roundTripWorksOnHeap(Allocator allocator) {
        assumeNativeMemory(allocator);
        Bytes<?> b = allocator.elasticBytes(32);
        try {
            TestObject source = new TestObject(123L, 123, false);
            int offset = BytesUtil.triviallyCopyableStart(source.getClass());
            b.unsafeWriteObject(source, offset, 13);
            TestObject dest = new TestObject();
            b.unsafeReadObject(dest, offset, 13);
            assertEquals(source,
                    dest,
                    "Unsafe copy should preserve the object fields");
        } finally {
            b.releaseLast();
        }
    }

    @ParameterizedTest(name = "allocator {0} writes length then reads")
    @DisplayName("writeWithLength encodes size and round trips content")
    @MethodSource("params")
    public void readWithLength(Allocator allocator) {
        assumeNativeMemory(allocator);
        int max = 130; // two bytes of length for a stop bit encoded length
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(max + 2);
        Bytes<?> from = Bytes.wrapForRead(new byte[max]);
        Bytes<?> to = Bytes.wrapForRead(new byte[max]);
        for (int len = 0; len <= max; len++) {
            from.readPositionRemaining(0, len);
            bytes.clear();
            bytes.writeWithLength(from);
            int expectedRemaining = len + (len < 128 ? 1 : 2);
            assertEquals(expectedRemaining,
                    bytes.readRemaining(),
                    "Encoded length should match expected size for len=" + len);
            bytes.readWithLength(to);
            assertEquals(len,
                    to.readRemaining(),
                    "Decoded length should match original size for len=" + len);
        }
    }

    static class TestObject {
        long l1;
        long i1;
        boolean b1;

        TestObject() {
        }

        TestObject(long l1, int i1, boolean b1) {
            this.l1 = l1;
            this.i1 = i1;
            this.b1 = b1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return l1 == that.l1 && i1 == that.i1 && b1 == that.b1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l1, i1, b1);
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "l1=" + l1 +
                    ", i1=" + i1 +
                    ", b1=" + b1 +
                    '}';
        }
    }
}
