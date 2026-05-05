/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BytesStore interface default methods covering branch paths for creation, wrapping,
 * atomic updates, and copy operations because complete coverage ensures reliable behaviour.
 */
@SuppressWarnings({"deprecation"})
@DisplayName("BytesStore interface default method branch coverage")
class BytesStoreBranchTest extends BytesTestCommon {

    @Test
    @DisplayName("from(CharSequence) with empty string returns zero-capacity store")
    void fromEmptyCharSequence() {
        BytesStore<?, ?> store = BytesStore.from("");
        assertEquals(0, store.capacity(),
                "from(CharSequence) should return zero-capacity store for empty string input");
    }

    @Test
    @DisplayName("from(CharSequence) with non-empty string should return store with content")
    void fromNonEmptyCharSequence() {
        BytesStore<?, ?> store = BytesStore.from("Hello");
        try {
            assertEquals(5, store.readRemaining(), "store should contain 5 bytes");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("from(CharSequence) with BytesStore returns a snapshot copy")
    void fromBytesStore() {
        Bytes<?> original = Bytes.allocateElasticOnHeap(16);
        try {
            original.append("Test");
            BytesStore<?, ?> copy = BytesStore.from((CharSequence) original);
            assertNotNull(copy, "BytesStore copy from CharSequence should be non-null");
            copy.releaseLast();
        } finally {
            original.releaseLast();
        }
    }

    @Test
    @DisplayName("from(String) with empty string returns zero-capacity store")
    void fromEmptyString() {
        BytesStore<?, ?> store = BytesStore.from("");
        assertEquals(0, store.capacity(),
                "from(String) should return zero-capacity store for empty string input");
    }

    @Test
    @DisplayName("from(String) with non-empty string returns store with content length")
    void fromNonEmptyString() {
        BytesStore<?, byte[]> store = BytesStore.from("Hello");
        try {
            assertEquals(5, store.capacity(), "store should have 5 byte capacity");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("wrap(ByteBuffer) with direct buffer should return native store")
    void wrapDirectByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(16);
        BytesStore<?, ByteBuffer> store = BytesStore.wrap(bb);
        try {
            assertTrue(store.isDirectMemory(),
                    "wrap(ByteBuffer) should keep direct-memory backing");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("wrap(ByteBuffer) with heap buffer should return heap store")
    void wrapHeapByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        BytesStore<?, ByteBuffer> store = BytesStore.wrap(bb);
        try {
            assertFalse(store.isDirectMemory(),
                    "wrap(ByteBuffer) should keep heap-memory backing");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("follow(ByteBuffer) with direct buffer should return native store")
    void followDirectByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(16);
        BytesStore<?, ByteBuffer> store = BytesStore.follow(bb);
        try {
            assertTrue(store.isDirectMemory(),
                    "follow(ByteBuffer) should keep direct-memory backing");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("follow(ByteBuffer) with heap buffer should return heap store")
    void followHeapByteBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(16);
        BytesStore<?, ByteBuffer> store = BytesStore.follow(bb);
        try {
            assertFalse(store.isDirectMemory(),
                    "follow(ByteBuffer) should keep heap-memory backing");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("empty() should return immutable zero-capacity BytesStore")
    void emptyStore() {
        BytesStore<?, ?> store = BytesStore.empty();
        assertEquals(0, store.capacity(), "empty store should have zero capacity");
    }

    @Test
    @DisplayName("wrap(byte[]) should create heap store")
    void wrapByteArray() {
        byte[] data = {1, 2, 3, 4, 5};
        BytesStore<?, byte[]> store = BytesStore.wrap(data);
        try {
            assertEquals(5, store.capacity(), "capacity should match array length");
            assertFalse(store.isDirectMemory(), "byte array store should not be direct memory");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeStore should create native memory store")
    void nativeStoreCreation() {
        BytesStore<?, Void> store = BytesStore.nativeStore(64);
        try {
            assertTrue(store.capacity() >= 64, "native store should have requested capacity");
            assertTrue(store.isDirectMemory(), "native store should be direct memory");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeStoreWithFixedCapacity should create fixed capacity store")
    void nativeStoreWithFixedCapacityCreation() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(32);
        try {
            assertEquals(32, store.capacity(), "fixed capacity should match requested");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSwapFloat should convert to int bits")
    void compareAndSwapFloat() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeFloat(0, 1.5f);
            assertTrue(store.compareAndSwapFloat(0, 1.5f, 2.5f),
                    "compareAndSwapFloat should succeed for expected 1.5f");
            assertEquals(2.5f, store.readFloat(0), 0.001f,
                    "compareAndSwapFloat should update stored float to 2.5f");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("compareAndSwapDouble should convert to long bits")
    void compareAndSwapDouble() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeDouble(0, 1.5);
            assertTrue(store.compareAndSwapDouble(0, 1.5, 2.5),
                    "compareAndSwapDouble should succeed when stored double equals 1.5");
            assertEquals(2.5, store.readDouble(0), 0.001,
                    "compareAndSwapDouble should update stored double to 2.5");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addAndGetInt adds delta and returns updated int value")
    void addAndGetInt() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeInt(0, 100);
            int result = store.addAndGetInt(0, 50);
            assertEquals(150, result,
                    "addAndGetInt should return 150 after adding integer delta to stored value");
            assertEquals(150, store.readInt(0),
                    "stored int value should update to 150 after addAndGetInt");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addAndGetLong adds delta and returns updated long value")
    void addAndGetLong() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeLong(0, 100L);
            long result = store.addAndGetLong(0, 50L);
            assertEquals(150L, result,
                    "addAndGetLong should return 150 after adding long delta to stored value");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addAndGetFloat adds delta and returns updated float value")
    void addAndGetFloat() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeFloat(0, 1.5f);
            float result = store.addAndGetFloat(0, 0.5f);
            assertEquals(2.0f, result, 0.001f,
                    "addAndGetFloat should return 2.0 after adding float delta to stored value");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addAndGetDouble adds delta and returns updated double value")
    void addAndGetDouble() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeDouble(0, 1.5);
            double result = store.addAndGetDouble(0, 0.5);
            assertEquals(2.0, result, 0.001,
                    "addAndGetDouble should return 2.0 after adding double delta to stored value");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesForRead should create readable view over stored content")
    void bytesForRead() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeLong(0, 0x1234567890ABCDEFL);
            Bytes<?> bytes = store.bytesForRead();
            assertEquals(0x1234567890ABCDEFL, bytes.readLong(),
                    "bytesForRead view should read the stored long value");
            bytes.releaseLast();
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesForWrite should create writable view over store content")
    void bytesForWrite() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            Bytes<?> bytes = store.bytesForWrite();
            bytes.writeLong(0x1234567890ABCDEFL);
            assertEquals(0x1234567890ABCDEFL, store.readLong(0),
                    "store should reflect written long value");
            bytes.releaseLast();
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("copyTo(BytesStore) should copy readable bytes")
    void copyToBytesStore() {
        BytesStore<?, Void> source = BytesStore.nativeStoreWithFixedCapacity(16);
        BytesStore<?, Void> target = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            source.writeLong(0, 0x1122334455667788L);
            long copied = source.copyTo(target);
            assertTrue(copied >= 8,
                    "copied " + copied + " bytes should be >= 8 for one long value");
        } finally {
            source.releaseLast();
            target.releaseLast();
        }
    }

    @Test
    @DisplayName("copyTo(OutputStream) should copy to stream")
    void copyToOutputStream() throws IOException {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5});
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            store.copyTo(baos);
            assertEquals(5, baos.size(), "all bytes should be copied");
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, baos.toByteArray(),
                    "output stream content should match input byte array");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("zeroOut should fill range with zeros")
    void zeroOutRange() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeLong(0, 0xFFFFFFFFFFFFFFFFL);
            store.writeLong(8, 0xFFFFFFFFFFFFFFFFL);
            store.zeroOut(4, 12);
            // Check the zeroed region
            assertEquals(0L, store.readLong(4), "zeroed region should be zero");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("zeroOut with start >= end should do nothing")
    void zeroOutNoOp() {
        BytesStore<?, Void> store = BytesStore.nativeStoreWithFixedCapacity(16);
        try {
            store.writeLong(0, 0x1234567890ABCDEFL);
            store.zeroOut(8, 4); // start >= end
            assertEquals(0x1234567890ABCDEFL, store.readLong(0),
                    "stored long value should remain unchanged after zeroOut no-op");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("length should return read remaining up to max int")
    void lengthReturnsReadRemaining() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[10]);
        try {
            assertEquals(10, store.length(), "length should equal capacity for full store");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("charAt should read stored byte as ASCII char")
    void charAtReadsByte() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        try {
            assertEquals('H', store.charAt(0), "charAt(0) should return 'H' for Hello");
            assertEquals('e', store.charAt(1), "second char should be e");
            assertEquals('o', store.charAt(4), "last char should be o");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("charAt should read valid index without throwing")
    void charAtValidIndex() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'i'});
        try {
            assertEquals('H', store.charAt(0), "charAt(0) should return 'H' for two-byte store");
            assertEquals('i', store.charAt(1), "second char should be i");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("subSequence should return a slice for requested range length")
    void subSequenceReturnsSlice() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        try {
            CharSequence sub = store.subSequence(1, 4);
            assertEquals(3, sub.length(), "slice should have length 3");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("subSequence with invalid start or end should throw IndexOutOfBoundsException")
    void subSequenceInvalidRange() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'i'});
        try {
            assertThrows(IndexOutOfBoundsException.class, () -> store.subSequence(-1, 2),
                    "subSequence should throw when start index is negative");
            assertThrows(IndexOutOfBoundsException.class, () -> store.subSequence(0, 10),
                    "subSequence should throw when end index exceeds store length");
            assertThrows(IndexOutOfBoundsException.class, () -> store.subSequence(2, 1),
                    "subSequence should throw when end index is less than start index");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("equalBytes should compare content for length 8")
    void equalBytesLengthEight() {
        BytesStore<?, byte[]> store1 = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        BytesStore<?, byte[]> store2 = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        BytesStore<?, byte[]> store3 = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 9});
        try {
            assertTrue(store1.equalBytes(store2, 8), "equal stores should match");
            assertFalse(store1.equalBytes(store3, 8), "different stores should not match");
        } finally {
            store1.releaseLast();
            store2.releaseLast();
            store3.releaseLast();
        }
    }

    @Test
    @DisplayName("equalBytes should compare content for other lengths")
    void equalBytesOtherLengths() {
        BytesStore<?, byte[]> store1 = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5});
        BytesStore<?, byte[]> store2 = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5});
        try {
            assertTrue(store1.equalBytes(store2, 5), "equalBytes should match for five-byte stores");
        } finally {
            store1.releaseLast();
            store2.releaseLast();
        }
    }

    @Test
    @DisplayName("byteCheckSum should compute checksum over all bytes")
    void byteCheckSumComputes() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5});
        try {
            int checksum = store.byteCheckSum();
            assertEquals((1 + 2 + 3 + 4 + 5) & 0xFF, checksum, "checksum should be sum of bytes");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("byteCheckSum with range should compute checksum for range")
    void byteCheckSumRange() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{1, 2, 3, 4, 5});
        try {
            int checksum = store.byteCheckSum(1, 4);
            assertEquals((2 + 3 + 4) & 0xFF, checksum, "checksum should be sum of bytes in range");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("endsWith should check last character in store content")
    void endsWithChecksLastChar() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        try {
            char expected = 'o';
            char unexpected = 'x';
            assertTrue(store.endsWith(expected),
                    "store " + store + " should end with '" + expected + "'");
            assertFalse(store.endsWith(unexpected),
                    "store " + store + " should not end with '" + unexpected + "'");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("endsWith on empty store returns false for any character")
    void endsWithEmptyStore() {
        BytesStore<?, ?> store = BytesStore.empty();
        char target = 'x';
        assertFalse(store.endsWith(target),
                "empty BytesStore " + store + " should not end with '" + target + "'");
    }

    @Test
    @DisplayName("startsWith should check first ASCII character in store")
    void startsWithChecksFirstChar() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        try {
            char expected = 'H';
            char unexpected = 'x';
            assertTrue(store.startsWith(expected),
                    "store " + store + " should start with '" + expected + "'");
            assertFalse(store.startsWith(unexpected),
                    "store " + store + " should not start with '" + unexpected + "'");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWith on empty store returns false for any character")
    void startsWithEmptyStore() {
        BytesStore<?, ?> store = BytesStore.empty();
        char target = 'x';
        assertFalse(store.startsWith(target),
                "empty BytesStore " + store + " should not start with '" + target + "'");
    }

    @Test
    @DisplayName("contentEquals should compare full content of two stores")
    void contentEqualsComparesFull() {
        BytesStore<?, byte[]> store1 = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        BytesStore<?, byte[]> store2 = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        BytesStore<?, byte[]> store3 = BytesStore.wrap(new byte[]{'W', 'o', 'r', 'l', 'd'});
        try {
            assertTrue(store1.contentEquals(store2), "equal content should match");
            assertFalse(store1.contentEquals(store3), "different content should not match");
        } finally {
            store1.releaseLast();
            store2.releaseLast();
            store3.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWith(BytesStore) should validate prefix store bytes")
    void startsWithBytesStore() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        BytesStore<?, byte[]> prefix = BytesStore.wrap(new byte[]{'H', 'e'});
        BytesStore<?, byte[]> notPrefix = BytesStore.wrap(new byte[]{'X', 'Y'});
        try {
            assertTrue(store.startsWith(prefix),
                    "store " + store + " should start with prefix " + prefix);
            assertFalse(store.startsWith(notPrefix),
                    "store " + store + " should not start with prefix " + notPrefix);
            assertFalse(store.startsWith(null),
                    "store " + store + " should not start with null prefix");
        } finally {
            store.releaseLast();
            prefix.releaseLast();
            notPrefix.releaseLast();
        }
    }

    @Test
    @DisplayName("to8bitString should convert bytes to 8-bit string")
    void to8bitStringConverts() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        try {
            assertEquals("Hello", store.to8bitString(),
                    "to8bitString should return Hello from 8-bit bytes");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("toUtf8String should convert bytes to UTF-8 string")
    void toUtf8StringConverts() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'H', 'e', 'l', 'l', 'o'});
        try {
            assertEquals("Hello", store.toUtf8String(),
                    "toUtf8String should return Hello from UTF-8 bytes");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("toDebugString should return a readable string representation")
    void toDebugStringReturnsRepresentation() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{'T', 'e', 's', 't'});
        try {
            String debug = store.toDebugString();
            assertNotNull(debug, "debug string should not be null");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("toDebugString with max length should limit output")
    void toDebugStringWithMaxLength() {
        byte[] largeData = new byte[1000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) ('A' + (i % 26));
        }
        BytesStore<?, byte[]> store = BytesStore.wrap(largeData);
        try {
            String debug = store.toDebugString(50);
            assertTrue(debug.length() <= 100, "debug string should be limited");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("inside(offset) should check bounds for single index")
    void insideChecksBounds() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[10]);
        try {
            assertTrue(store.inside(0), "0 should be inside");
            assertTrue(store.inside(5), "5 should be inside");
            assertFalse(store.inside(10), "10 should be outside (exclusive)");
            assertFalse(store.inside(-1), "negative should be outside");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("inside(offset, bufferSize) should check range")
    void insideChecksRange() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[10]);
        try {
            assertTrue(store.inside(0, 5), "0-5 should be inside");
            assertTrue(store.inside(5, 5), "5-10 should be inside");
            assertFalse(store.inside(5, 6), "5-11 should be outside");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("addAndGetUnsignedByteNotAtomic adds delta and returns updated value")
    void addAndGetUnsignedByteNotAtomic() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[]{10});
        try {
            int result = store.addAndGetUnsignedByteNotAtomic(0, 5);
            assertEquals(15, result, "unsigned add should return 15 after adding 5 to 10");
            assertEquals(15, store.readUnsignedByte(0),
                    "stored unsigned byte should update to 15");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativePointer should create uninitialised PointerBytesStore instance")
    void nativePointerCreation() {
        PointerBytesStore pbs = BytesStore.nativePointer();
        assertNotNull(pbs, "nativePointer should return non-null PointerBytesStore");
        // No need to release - not initialized
    }

    @Test
    @DisplayName("elasticByteBuffer should create elastic buffer with capacity range")
    void elasticByteBufferCreation() {
        BytesStore<?, ByteBuffer> store = BytesStore.elasticByteBuffer(16, 1024);
        try {
            assertTrue(store.capacity() >= 16, "capacity should be at least initial size");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("nativeStoreFrom should create store from byte array")
    void nativeStoreFromByteArray() {
        BytesStore<?, Void> store = BytesStore.nativeStoreFrom(new byte[]{1, 2, 3, 4, 5});
        try {
            assertEquals(5, store.capacity(),
                    "nativeStoreFrom should report capacity 5 for five-byte array");
            assertEquals(1, store.readByte(0), "nativeStoreFrom should copy byte array so readByte(0) returns 1");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesStore should return the same BytesStore identity reference instance")
    void bytesStoreReturnsSelf() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[10]);
        try {
            assertSame(store, store.bytesStore(), "bytesStore should return the same instance");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("realCapacity should equal capacity by default")
    void realCapacityEqualsCapacity() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[10]);
        try {
            assertEquals(store.capacity(), store.realCapacity(), "realCapacity should equal capacity");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("safeLimit should equal capacity by default")
    void safeLimitEqualsCapacity() {
        BytesStore<?, byte[]> store = BytesStore.wrap(new byte[10]);
        try {
            assertEquals(store.capacity(), store.safeLimit(), "safeLimit should equal capacity");
        } finally {
            store.releaseLast();
        }
    }
}
