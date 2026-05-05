/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests streaming data input operations because correct position tracking
 * and bounded views are essential to avoid reading past valid data.
 * This test verifies position management, unsafe copy round trips, and
 * length-prefixed reads so that consumers can safely parse serialised data.
 */
@DisplayName("StreamingDataInput - validates read operations and position management")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class StreamingDataInputTest extends BytesTestCommon {

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

    @Test
    @DisplayName("readPositionForHeader returns current position without skip padding")
    void readPositionForHeaderNoSkip() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("0123456789");
            bytes.readPosition(3);
            long pos = bytes.readPositionForHeader(false);
            assertEquals(3, pos, "readPositionForHeader should return current position without padding");
            assertEquals(3, bytes.readPosition(), "Read position should not change");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readPositionForHeader with skip padding aligns to boundary")
    void readPositionForHeaderWithSkip() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("0123456789ABCDEF");
            bytes.readPosition(1); // Not aligned to 4-byte boundary
            long pos = bytes.readPositionForHeader(true);
            // Position 1 should skip 3 bytes to reach position 4 (next 4-byte boundary)
            assertEquals(4, pos, "readPositionForHeader with skip should align to boundary");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readWithLength supplies consumer with bounded view")
    void readWithLengthConsumer() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("0123456789");
            bytes.readPosition(0);
            AtomicInteger readCount = new AtomicInteger();
            bytes.readWithLength(5, b -> {
                assertEquals(5, b.readRemaining(),
                        "readWithLength should expose bounded remaining=5 to consumer");
                readCount.incrementAndGet();
            });
            assertEquals(1, readCount.get(), "readWithLength should call consumer once");
            assertEquals(5, bytes.readPosition(), "readWithLength should advance read position by 5");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readWithLength throws when length exceeds remaining")
    void readWithLengthThrowsOnOverflow() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("abc");
            bytes.readPosition(0);
            assertThrows(BufferUnderflowException.class,
                    () -> bytes.readWithLength(10, b -> {}),
                    "readWithLength should throw when length exceeds remaining");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readWithLength0 supplies consumer with bounded view")
    void readWithLength0Consumer() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        Bytes<?> output = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("0123456789");
            bytes.readPosition(0);
            AtomicInteger readCount = new AtomicInteger();
            bytes.readWithLength0(5, (b, sb, out) -> {
                assertEquals(5, b.readRemaining(),
                        "readWithLength0 should expose bounded remaining=5 to consumer");
                readCount.incrementAndGet();
            }, null, output);
            assertEquals(1, readCount.get(), "readWithLength0 should call consumer once");
            assertEquals(5, bytes.readPosition(), "readWithLength0 should advance read position by 5");
        } finally {
            bytes.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("readWithLength0 throws when length exceeds remaining")
    void readWithLength0ThrowsOnOverflow() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        Bytes<?> output = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("abc");
            bytes.readPosition(0);
            assertThrows(BufferUnderflowException.class,
                    () -> bytes.readWithLength0(10, (b, sb, out) -> {}, null, output),
                    "readWithLength0 should throw when length exceeds remaining");
        } finally {
            bytes.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("readPositionUnlimited sets position beyond current read limit")
    void readPositionUnlimitedTest() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("0123456789ABCDEF");
            bytes.readLimit(5); // Restrict limit
            bytes.readPositionUnlimited(10); // Should work past the limit
            assertEquals(10, bytes.readPosition(),
                    "readPositionUnlimited should set position to 10 when limit is 5");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readPositionRemaining sets both position and limit")
    void readPositionRemainingTest() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("0123456789ABCDEF");
            bytes.readPositionRemaining(2, 5);
            assertEquals(2, bytes.readPosition(),
                    "readPositionRemaining should set read position to 2");
            assertEquals(5, bytes.readRemaining(),
                    "readPositionRemaining should set remaining to 5");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readLimitToCapacity expands limit to full capacity")
    void readLimitToCapacityTest() {
        // Use fixed capacity bytes to test readLimitToCapacity
        Bytes<?> bytes = Bytes.wrapForRead(new byte[16]);
        try {
            bytes.readLimit(5);
            assertEquals(5, bytes.readRemaining(), "Initial limit should be respected");
            bytes.readLimitToCapacity();
            assertEquals(16, bytes.readRemaining(), "Limit should expand to full capacity");
        } finally {
            bytes.releaseLast();
        }
    }
}
