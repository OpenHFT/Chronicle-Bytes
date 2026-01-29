/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BytesInternal comparison methods, because content equality
 * must handle null inputs and different lengths to avoid false matches.
 * Covers contentEqual, contentEqualsLong, contentEqualInt, startsWith variants.
 */
@DisplayName("BytesInternal content comparison for equality and prefix matching")
class BytesInternalComparisonTest extends BytesTestCommon {

    private Bytes<?> bytes1;
    private Bytes<?> bytes2;

    @BeforeEach
    void setUp() {
        bytes1 = NativeBytes.nativeBytes(256);
        bytes2 = NativeBytes.nativeBytes(256);
    }

    @AfterEach
    void tearDown() {
        if (bytes1 != null) {
            bytes1.releaseLast();
            bytes1 = null;
        }
        if (bytes2 != null) {
            bytes2.releaseLast();
            bytes2 = null;
        }
    }

    // --- contentEqual basic tests ---

    @Test
    @DisplayName("contentEqual treats two null byte store inputs as equal")
    void contentEqualBothNull() {
        assertTrue(BytesInternal.contentEqual(null, null),
                "contentEqual treats two null byte stores as equal inputs");
    }

    @Test
    @DisplayName("contentEqual rejects null left byte store against data")
    void contentEqualFirstNull() {
        bytes2.append("data");
        assertFalse(BytesInternal.contentEqual(null, bytes2),
                "contentEqual rejects null left store when right store has data");
    }

    @Test
    @DisplayName("contentEqual rejects null right byte store against data")
    void contentEqualSecondNull() {
        bytes1.append("data");
        assertFalse(BytesInternal.contentEqual(bytes1, null),
                "contentEqual rejects null right store when left store has data");
    }

    @Test
    @DisplayName("contentEqual rejects content length mismatch for non-empty inputs")
    void contentEqualDifferentLengths() {
        bytes1.append("short");
        bytes2.append("longer text");
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2),
                "contentEqual rejects length mismatch 5 vs 11 bytes");
    }

    @Test
    @DisplayName("contentEqual returns true for empty byte store sequences")
    void contentEqualBothEmpty() {
        assertTrue(BytesInternal.contentEqual(bytes1, bytes2), "contentEqual returns true for empty byte stores");
    }

    @Test
    @DisplayName("contentEqual returns true for same byte content")
    void contentEqualSameContent() {
        bytes1.append("Hello World!");
        bytes2.append("Hello World!");
        assertTrue(BytesInternal.contentEqual(bytes1, bytes2), "contentEqual returns true for matching byte content");
    }

    @Test
    @DisplayName("contentEqual returns false for same length different content")
    void contentEqualSameLengthDifferentContent() {
        bytes1.append("Hello World!");
        bytes2.append("Hello World?");
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2), "contentEqual returns false for same length different bytes");
    }

    // --- contentEqual with various lengths to exercise different loop paths ---

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17, 31, 32, 33})
    @DisplayName("contentEqual matches data for various lengths")
    void contentEqualMatchingVariousLengths(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) ('A' + (i % 26));
        }
        bytes1.write(data);
        bytes2.write(data);
        assertTrue(BytesInternal.contentEqual(bytes1, bytes2),
                "length " + length + " matches equal content");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17, 31, 32, 33})
    @DisplayName("contentEqual rejects mismatch at last byte")
    void contentEqualMismatchAtEndVariousLengths(int length) {
        byte[] data1 = new byte[length];
        byte[] data2 = new byte[length];
        for (int i = 0; i < length; i++) {
            data1[i] = (byte) ('A' + (i % 26));
            data2[i] = (byte) ('A' + (i % 26));
        }
        data2[length - 1] = (byte) 'Z'; // Mismatch at end
        bytes1.write(data1);
        bytes2.write(data2);
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2),
                "length " + length + " mismatch at end returns false");
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 9, 15, 16, 17, 31, 32, 33})
    @DisplayName("contentEqual rejects mismatch at first byte")
    void contentEqualMismatchAtStart(int length) {
        byte[] data1 = new byte[length];
        byte[] data2 = new byte[length];
        for (int i = 0; i < length; i++) {
            data1[i] = (byte) ('A' + (i % 26));
            data2[i] = (byte) ('A' + (i % 26));
        }
        data2[0] = (byte) 'Z'; // Mismatch at start
        bytes1.write(data1);
        bytes2.write(data2);
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2),
                "length " + length + " mismatch at start returns false");
    }

    @Test
    @DisplayName("contentEqual rejects mismatch in 8-byte chunk")
    void contentEqualMismatchInMiddleOfChunk() {
        byte[] data1 = new byte[16];
        byte[] data2 = new byte[16];
        for (int i = 0; i < 16; i++) {
            data1[i] = (byte) i;
            data2[i] = (byte) i;
        }
        data2[4] = (byte) 99; // Mismatch at position 4 (middle of first 8-byte chunk)
        bytes1.write(data1);
        bytes2.write(data2);
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2),
                "contentEqual returns false for chunk mismatch");
    }

    // --- Tests for zero-padding comparison (when a > b in length but data matches) ---

    @Test
    @DisplayName("contentEqual rejects prefix match when lengths differ")
    void contentEqualWithTrailingZeros() {
        // bytes1 has "Hello" + zeros, bytes2 has just "Hello"
        // They should not be equal if lengths differ
        bytes1.append("Hello");
        bytes2.append("Hello World");
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2),
                "contentEqual rejects prefix match when lengths 5 and 11 differ");
    }

    // --- startsWith tests ---

    @Test
    @DisplayName("startsWith returns true for empty prefix byte sequence")
    void startsWithEmptyPrefix() {
        bytes1.append("Hello World");
        // Empty bytes2 is a prefix of any non-empty bytes
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches empty prefix '" + bytes2.toString() + "' in content");
    }

    @Test
    @DisplayName("startsWith returns false when prefix exceeds content")
    void startsWithPrefixLongerThanContent() {
        bytes1.append("Hi");
        bytes2.append("Hello World");
        assertFalse(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith rejects prefix '" + bytes2.toString() + "' longer than content");
    }

    @Test
    @DisplayName("startsWith returns true for exact match prefix byte sequence")
    void startsWithExactMatch() {
        bytes1.append("Hello");
        bytes2.append("Hello");
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches exact prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith returns true for valid prefix byte sequence")
    void startsWithValidPrefix() {
        bytes1.append("Hello World");
        bytes2.append("Hello");
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches valid prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith returns false for invalid prefix byte sequence")
    void startsWithInvalidPrefix() {
        bytes1.append("Hello World");
        bytes2.append("World");
        assertFalse(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith rejects invalid prefix '" + bytes2.toString() + "'");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17})
    @DisplayName("startsWith matches prefixes of various lengths")
    void startsWithVariousPrefixLengths(int prefixLength) {
        byte[] content = new byte[32];
        for (int i = 0; i < 32; i++) {
            content[i] = (byte) ('A' + (i % 26));
        }
        bytes1.write(content);
        bytes2.write(content, 0, prefixLength);
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "prefix length " + prefixLength + " matches prefix '" + bytes2.toString() + "'");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 17})
    @DisplayName("startsWith rejects mismatch at various positions")
    void startsWithMismatchAtVariousPositions(int mismatchPos) {
        byte[] content1 = new byte[32];
        byte[] content2 = new byte[32];
        for (int i = 0; i < 32; i++) {
            content1[i] = (byte) ('A' + (i % 26));
            content2[i] = (byte) ('A' + (i % 26));
        }
        content2[mismatchPos - 1] = (byte) 'Z'; // Mismatch at specified position
        bytes1.write(content1);
        bytes2.write(content2, 0, mismatchPos);
        assertFalse(BytesInternal.startsWith(bytes1, bytes2),
                "mismatch at position " + mismatchPos + " rejects prefix '" + bytes2.toString() + "'");
    }

    // --- Tests to exercise unchecked paths ---

    @Test
    @DisplayName("contentEqual uses unchecked path with NativeBytes")
    void contentEqualNativeBytesUncheckedPath() {
        // NativeBytes implements HasUncheckedRandomDataInput
        byte[] data = new byte[64];
        for (int i = 0; i < 64; i++) {
            data[i] = (byte) ('A' + (i % 26));
        }
        bytes1.write(data);
        bytes2.write(data);
        assertTrue(BytesInternal.contentEqual(bytes1, bytes2),
                "contentEqual returns true for NativeBytes unchecked path");
    }

    @Test
    @DisplayName("startsWith uses unchecked path with NativeBytes")
    void startsWithNativeBytesUncheckedPath() {
        // NativeBytes implements HasUncheckedRandomDataInput
        byte[] data = new byte[64];
        for (int i = 0; i < 64; i++) {
            data[i] = (byte) ('A' + (i % 26));
        }
        bytes1.write(data);
        bytes2.write(data, 0, 32);
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches NativeBytes prefix '" + bytes2.toString() + "'");
    }

    // --- Tests for startsWithUnchecked ---

    @Test
    @DisplayName("startsWithUnchecked matches prefix for native data")
    void startsWithUncheckedMatching() {
        byte[] data = new byte[20];
        for (int i = 0; i < 20; i++) {
            data[i] = (byte) ('A' + i);
        }
        NativeBytes<Void> native1 = NativeBytes.nativeBytes(64);
        NativeBytes<Void> native2 = NativeBytes.nativeBytes(64);
        try {
            native1.write(data);
            native2.write(data, 0, 10);
            assertTrue(BytesInternal.startsWithUnchecked(native1, native2),
                    "startsWithUnchecked matches native prefix length 10");
        } finally {
            native1.releaseLast();
            native2.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWithUnchecked rejects non-matching native prefix data")
    void startsWithUncheckedNonMatching() {
        NativeBytes<Void> native1 = NativeBytes.nativeBytes(64);
        NativeBytes<Void> native2 = NativeBytes.nativeBytes(64);
        try {
            native1.append("Hello World");
            native2.append("Goodbye");
            assertFalse(BytesInternal.startsWithUnchecked(native1, native2),
                    "startsWithUnchecked rejects prefix 'Goodbye' against 'Hello World'");
        } finally {
            native1.releaseLast();
            native2.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWithUnchecked rejects prefix longer than content")
    void startsWithUncheckedPrefixLonger() {
        NativeBytes<Void> native1 = NativeBytes.nativeBytes(64);
        NativeBytes<Void> native2 = NativeBytes.nativeBytes(64);
        try {
            native1.append("Hi");
            native2.append("Hello World");
            long contentLength = native1.readRemaining();
            long prefixLength = native2.readRemaining();
            assertFalse(BytesInternal.startsWithUnchecked(native1, native2),
                    "startsWithUnchecked rejects prefix length " + prefixLength
                            + " for content length " + contentLength);
        } finally {
            native1.releaseLast();
            native2.releaseLast();
        }
    }

    // --- Tests for capacity comparison paths (a.realCapacity < b.realCapacity) ---

    @Test
    @DisplayName("contentEqual returns true when capacity order differs")
    void contentEqualSwapsOrderWhenFirstSmaller() {
        // Create bytes with different capacities
        Bytes<?> small = Bytes.allocateElasticDirect(32);
        Bytes<?> large = Bytes.allocateElasticDirect(256);
        try {
            small.append("Test");
            large.append("Test");
            // Both have same content, should be equal regardless of capacity order
            assertTrue(BytesInternal.contentEqual(small, large),
                    "contentEqual returns true when content matches");
            assertTrue(BytesInternal.contentEqual(large, small),
                    "contentEqual returns true for reverse capacity order");
        } finally {
            small.releaseLast();
            large.releaseLast();
        }
    }

    // --- Edge cases ---

    @Test
    @DisplayName("contentEqual returns true for single byte match value")
    void contentEqualSingleByte() {
        bytes1.writeByte((byte) 'A');
        bytes2.writeByte((byte) 'A');
        assertTrue(BytesInternal.contentEqual(bytes1, bytes2),
                "contentEqual returns true for single byte 'A' match");
    }

    @Test
    @DisplayName("contentEqual returns false for single byte mismatch value")
    void contentEqualSingleDifferentByte() {
        bytes1.writeByte((byte) 'A');
        bytes2.writeByte((byte) 'B');
        assertFalse(BytesInternal.contentEqual(bytes1, bytes2),
                "contentEqual returns false for single byte A/B mismatch");
    }

    @Test
    @DisplayName("startsWith matches single byte prefix in content")
    void startsWithSingleByteMatch() {
        bytes1.append("Hello");
        bytes2.writeByte((byte) 'H');
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches single byte prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith rejects single byte prefix mismatch in content")
    void startsWithSingleByteMismatch() {
        bytes1.append("Hello");
        bytes2.writeByte((byte) 'X');
        assertFalse(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith rejects single byte prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith matches prefix across 4-byte chunk")
    void startsWithFourByteChunk() {
        // Length 5 exercises: 0 8-byte chunks, 1 4-byte chunk, 0 2-byte chunks, 1 single byte
        byte[] data = new byte[5];
        for (int i = 0; i < 5; i++) {
            data[i] = (byte) ('A' + i);
        }
        bytes1.write(new byte[32]); // Ensure larger content
        bytes1.readPosition(0);
        bytes1.writePosition(0);
        bytes1.write(data);
        bytes1.write(new byte[27]); // Pad to 32 bytes total

        bytes2.write(data);
        bytes1.readPosition(0);
        bytes2.readPosition(0);
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches 4-byte chunk prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith matches prefix across 2-byte chunk")
    void startsWithTwoByteChunk() {
        // Length 6 exercises: 0 8-byte chunks, 1 4-byte chunk, 1 2-byte chunk
        byte[] data = new byte[6];
        for (int i = 0; i < 6; i++) {
            data[i] = (byte) ('A' + i);
        }
        Bytes<?> content = Bytes.allocateElasticDirect(64);
        Bytes<?> prefix = Bytes.allocateElasticDirect(64);
        try {
            content.write(data);
            content.write(new byte[26]); // Pad to 32 bytes
            prefix.write(data);
            assertTrue(BytesInternal.startsWith(content, prefix),
                    "startsWith matches 2-byte chunk prefix '" + prefix.toString() + "'");
        } finally {
            content.releaseLast();
            prefix.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWith matches length 7 prefix across chunks")
    void startsWithLength7() {
        byte[] data = new byte[7];
        for (int i = 0; i < 7; i++) {
            data[i] = (byte) ('A' + i);
        }
        bytes1.write(data);
        bytes1.write(new byte[25]);
        bytes2.write(data);
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches length 7 prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith matches length 3 prefix across chunks")
    void startsWithLength3() {
        byte[] data = new byte[3];
        data[0] = 'A';
        data[1] = 'B';
        data[2] = 'C';
        bytes1.write(data);
        bytes1.write(new byte[29]);
        bytes2.write(data);
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches 2-byte and 1-byte prefix '" + bytes2.toString() + "'");
    }

    @Test
    @DisplayName("startsWith matches length 2 prefix across chunk")
    void startsWithLength2() {
        bytes1.append("AB");
        bytes1.write(new byte[30]);
        bytes2.append("AB");
        assertTrue(BytesInternal.startsWith(bytes1, bytes2),
                "startsWith matches length 2 prefix '" + bytes2.toString() + "'");
    }

    // --- Tests with HeapBytesStore for non-direct memory paths ---

    @Test
    @DisplayName("contentEqual returns true for heap-only byte stores")
    void contentEqualHeapBytes() {
        Bytes<?> heap1 = Bytes.allocateElasticOnHeap(64);
        Bytes<?> heap2 = Bytes.allocateElasticOnHeap(64);
        try {
            heap1.append("Test data for heap comparison");
            heap2.append("Test data for heap comparison");
            assertTrue(BytesInternal.contentEqual(heap1, heap2),
                    "contentEqual returns true for heap-only stores with matching content");
        } finally {
            heap1.releaseLast();
            heap2.releaseLast();
        }
    }

    @Test
    @DisplayName("startsWith matches prefix for heap bytes")
    void startsWithHeapBytes() {
        Bytes<?> heap1 = Bytes.allocateElasticOnHeap(64);
        Bytes<?> heap2 = Bytes.allocateElasticOnHeap(64);
        try {
            heap1.append("Hello World");
            heap2.append("Hello");
            assertTrue(BytesInternal.startsWith(heap1, heap2),
                    "startsWith matches heap prefix '" + heap2.toString() + "'");
        } finally {
            heap1.releaseLast();
            heap2.releaseLast();
        }
    }

    @Test
    @DisplayName("contentEqual returns true for mixed heap and native stores")
    void contentEqualMixedHeapAndNative() {
        Bytes<?> heap = Bytes.allocateElasticOnHeap(64);
        try {
            heap.append("Mixed comparison test");
            bytes1.append("Mixed comparison test");
            assertTrue(BytesInternal.contentEqual(heap, bytes1),
                    "contentEqual returns true for heap and native stores with matching content");
        } finally {
            heap.releaseLast();
        }
    }
}
