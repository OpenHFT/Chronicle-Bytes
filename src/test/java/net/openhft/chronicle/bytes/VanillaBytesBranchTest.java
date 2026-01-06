/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VanillaBytes covering branch coverage for read and write operations,
 * comparisons, and buffer-backed behaviours.
 */
@DisplayName("VanillaBytes branch coverage for read, write, and comparison operations")
class VanillaBytesBranchTest extends BytesTestCommon {

    private Bytes<?> bytes;

    @BeforeEach
    void setUp() {
        // Use elastic direct for most tests - VanillaBytes is the base impl
        bytes = Bytes.allocateElasticDirect(256);
    }

    @AfterEach
    void tearDown() {
        if (bytes != null) {
            bytes.releaseLast();
        }
    }

    // ========== Basic Properties ==========

    @Test
    @DisplayName("isElastic returns false for VanillaBytes.wrap on fixed native store")
    void shouldReturnFalseForIsElastic() {
        BytesStore<?, ?> store = BytesStore.nativeStore(64);
        VanillaBytes<?> vanilla = VanillaBytes.wrap(store);
        try {
            assertFalse(vanilla.isElastic(),
                    "VanillaBytes.wrap should return false for isElastic");
        } finally {
            vanilla.releaseLast();
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("vanillaBytes() creates an empty VanillaBytes instance with zero capacity")
    void shouldCreateEmptyVanillaBytes() {
        VanillaBytes<Void> vanilla = VanillaBytes.vanillaBytes();
        try {
            assertNotNull(vanilla, "vanillaBytes() should create non-null instance");
        } finally {
            vanilla.releaseLast();
        }
    }

    // ========== Position and Limit Operations ==========

    @Test
    @DisplayName("Byteable offset returns the current readPosition value")
    void shouldReturnReadPositionAsOffset() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.readPosition(4);

        Byteable byteable = (Byteable) bytes;
        assertEquals(4, byteable.offset(),
                "Byteable offset should equal the current read position");
    }

    @Test
    @DisplayName("Byteable maxSize returns the current readRemaining value")
    void shouldReturnReadRemainingAsMaxSize() {
        bytes.write(new byte[20]);
        bytes.readPosition(5);

        Byteable byteable = (Byteable) bytes;
        assertEquals(bytes.readRemaining(), byteable.maxSize(),
                "Byteable maxSize should match the current read remaining count");
    }

    @Test
    @DisplayName("realCapacity returns the underlying store capacity for Bytes")
    void shouldReturnRealCapacity() {
        assertTrue(bytes.realCapacity() > 0,
                "Bytes realCapacity should report a positive value");
    }

    // ========== Byteable Interface ==========

    @Test
    @DisplayName("Byteable offset matches readPosition after manual advance")
    void shouldReturnReadPositionFromByteable() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.readPosition(4);

        Byteable byteable = (Byteable) bytes;
        assertEquals(4, byteable.offset(),
                "Byteable offset should match the read position value");
    }

    // ========== String Comparison ==========

    @Test
    @DisplayName("isEqual returns true for matching ASCII string input")
    void shouldReturnTrueForMatchingString() {
        bytes.append8bit("Hello");

        assertTrue(bytes.isEqual("Hello"),
                "Bytes.isEqual should return true for the matching string");
    }

    @Test
    @DisplayName("isEqual returns false for null CharSequence reference argument")
    void shouldReturnFalseForNullString() {
        bytes.append8bit("Hello");

        assertFalse(bytes.isEqual(null),
                "Bytes.isEqual should return false for null input");
    }

    @Test
    @DisplayName("isEqual returns false for shorter input length than content")
    void shouldReturnFalseForDifferentLength() {
        bytes.append8bit("Hello");

        assertFalse(bytes.isEqual("Hi"),
                "Bytes.isEqual should return false when the input length differs");
    }

    @Test
    @DisplayName("isEqual returns false for different content bytes")
    void shouldReturnFalseForDifferentContent() {
        bytes.append8bit("Hello");

        assertFalse(bytes.isEqual("World"),
                "Bytes.isEqual should return false for different content values");
    }

    // ========== findByte ==========

    @Test
    @DisplayName("findByte locates target byte within ASCII content")
    void shouldFindByte() {
        bytes.write(new byte[]{'H', 'e', 'l', 'l', 'o'});
        bytes.readPosition(0);

        long pos = bytes.findByte((byte) 'l');

        assertEquals(2, pos, "findByte should locate 'l' at position 2 in Hello");
    }

    @Test
    @DisplayName("findByte returns -1 when target byte is absent")
    void shouldReturnMinusOneWhenByteNotFound() {
        bytes.write(new byte[]{'H', 'e', 'l', 'l', 'o'});
        bytes.readPosition(0);

        long pos = bytes.findByte((byte) 'x');

        assertEquals(-1, pos, "findByte should return -1 when the target byte is absent");
    }

    // ========== parseLong ==========

    @Test
    @DisplayName("parseLong parses signed decimal number from ASCII content")
    void shouldParseLong() {
        bytes.append("12345");
        bytes.readPosition(0);

        long result = bytes.parseLong();

        assertEquals(12345L, result, "parseLong should parse 12345 from ASCII digits");
    }

    @Test
    @DisplayName("parseLong parses negative decimal values from ASCII digits")
    void shouldParseLongNegative() {
        bytes.append("-9876");
        bytes.readPosition(0);

        long result = bytes.parseLong();

        assertEquals(-9876L, result, "parseLong should parse -9876 from ASCII digits");
    }

    // ========== bytesForRead ==========

    @Test
    @DisplayName("bytesForRead returns VanillaBytes view when readPosition is cleared")
    void shouldReturnVanillaBytesWhenClear() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.clear();
        bytes.writeLong(0x123456789ABCDEF0L);

        Bytes<?> forRead = bytes.bytesForRead();
        try {
            assertNotNull(forRead, "bytesForRead should return a non-null view after clear");
        } finally {
            forRead.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesForRead returns SubBytes view when readPosition is advanced")
    void shouldReturnSubBytesWhenNotClear() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.readPosition(2); // Not at start

        Bytes<?> forRead = bytes.bytesForRead();
        try {
            assertNotNull(forRead, "bytesForRead should return a non-null view when advanced");
            assertTrue(forRead instanceof SubBytes,
                    "bytesForRead should return SubBytes when read position is not at start");
        } finally {
            forRead.releaseLast();
        }
    }

    // ========== copy ==========

    @Test
    @DisplayName("copy creates an independent BytesStore snapshot of content")
    void shouldCreateIndependentCopy() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.readPosition(0);

        BytesStore<?, ?> copy = bytes.copy();
        try {
            assertNotNull(copy, "copy should return a non-null BytesStore snapshot");
            assertEquals(0x123456789ABCDEF0L, copy.readLong(0),
                    "copy should preserve the stored long value");
        } finally {
            copy.releaseLast();
        }
    }

    @Test
    @DisplayName("copy works with ByteBuffer-backed Bytes instances")
    void shouldCopyWithByteBufferBacking() {
        ByteBuffer bb = ByteBuffer.allocateDirect(64);
        bb.putLong(0x123456789ABCDEF0L);
        bb.flip();

        Bytes<?> bbBytes = Bytes.wrapForRead(bb);
        try {
            BytesStore<?, ?> copy = bbBytes.copy();
            try {
                assertNotNull(copy, "ByteBuffer-backed copy should be a non-null instance");
            } finally {
                copy.releaseLast();
            }
        } finally {
            bbBytes.releaseLast();
        }
    }

    // ========== append CharSequence ==========

    @Test
    @DisplayName("append(CharSequence, start, end) with BytesStore")
    void shouldAppendBytesStoreCharSequence() {
        Bytes<?> source = Bytes.from("Hello, World!");
        try {
            bytes.append((CharSequence) source, 0, 5);

            bytes.readPosition(0);
            assertEquals(5, bytes.readRemaining(),
                    "append(CharSequence, start, end) should write 5 bytes");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("append(CharSequence, start, end) with String")
    void shouldAppendStringCharSequence() {
        bytes.append("Hello, World!", 7, 12);

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (bytes.readRemaining() > 0) {
            sb.append((char) bytes.readByte());
        }
        assertEquals("World", sb.toString(),
                "append(CharSequence, start, end) should append substring World");
    }

    // ========== appendUtf8 ==========

    @Test
    @DisplayName("appendUtf8 with String writes UTF-8 bytes to target")
    void shouldAppendUtf8String() {
        bytes.appendUtf8("Hello");

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (bytes.readRemaining() > 0) {
            sb.append((char) bytes.readByte());
        }
        assertEquals("Hello", sb.toString(),
                "appendUtf8 should write UTF-8 bytes for Hello");
    }

    @Test
    @DisplayName("appendUtf8 with BytesStore appends UTF-8 bytes from source")
    void shouldAppendUtf8BytesStore() {
        Bytes<?> source = Bytes.from("Test");
        try {
            bytes.appendUtf8(source);

            bytes.readPosition(0);
            assertEquals(4, bytes.readRemaining(),
                    "appendUtf8(BytesStore) should append 4 bytes total");
        } finally {
            source.releaseLast();
        }
    }

    // ========== append8bit ==========

    @Test
    @DisplayName("append8bit with CharSequence writes 8-bit characters to Bytes")
    void shouldAppend8bitCharSequence() {
        bytes.append8bit("Test data");

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (bytes.readRemaining() > 0) {
            sb.append((char) bytes.readByte());
        }
        assertEquals("Test data", sb.toString(),
                "append8bit should write 8-bit characters for Test data");
    }

    @Test
    @DisplayName("append8bit with String on direct memory")
    void shouldAppend8bitStringOnDirectMemory() {
        assertTrue(bytes.isDirectMemory(),
                "bytes instance should be backed by direct memory");
        bytes.append8bit("Direct test");

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (bytes.readRemaining() > 0) {
            sb.append((char) bytes.readByte());
        }
        assertEquals("Direct test", sb.toString(),
                "append8bit should write 8-bit string on direct memory");
    }

    @Test
    @DisplayName("append8bit with BytesStore appends 8-bit bytes from source")
    void shouldAppend8bitBytesStore() {
        Bytes<?> source = Bytes.from("Source");
        try {
            bytes.append8bit(source);

            bytes.readPosition(0);
            assertEquals(6, bytes.readRemaining(),
                    "append8bit(BytesStore) should append 6 bytes total");
        } finally {
            source.releaseLast();
        }
    }

    // ========== write(BytesStore) ==========

    @Test
    @DisplayName("write(BytesStore, offset, length) copies data")
    void shouldWriteBytesStoreRange() {
        Bytes<?> source = Bytes.allocateElasticDirect(64);
        try {
            source.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

            bytes.write((BytesStore<?, ?>) source, 2L, 5L);

            bytes.readPosition(0);
        assertEquals(3, bytes.readByte(), "write(BytesStore, offset, length) should copy byte 3 first");
        assertEquals(5, bytes.readRemaining() + 1,
                "write(BytesStore, offset, length) should write 5 bytes total");
        } finally {
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("write uses direct memory optimisation for native BytesStore")
    void shouldWriteWithDirectOptimisation() {
        Bytes<?> source = Bytes.allocateElasticDirect(64);
        try {
            source.writeLong(0x123456789ABCDEF0L);
            source.readPosition(0);

            bytes.write((BytesStore<?, ?>) source, 0L, 8L);

            bytes.readPosition(0);
        assertEquals(0x123456789ABCDEF0L, bytes.readLong(),
                "write should copy the long value without changes");
        } finally {
            source.releaseLast();
        }
    }

    // ========== write(CharSequence) at position ==========

    @Test
    @DisplayName("write(position, CharSequence, offset, length) writes at position")
    void shouldWriteCharSequenceAtPosition() {
        VanillaBytes<?> vanilla = (VanillaBytes<?>) bytes;
        vanilla.write(10, "Hello, World!", 0, 5);

        assertEquals('H', bytes.readByte(10), "write(position, ...) should write 'H' at position 10");
        assertEquals('e', bytes.readByte(11), "write(position, ...) should write 'e' at position 11");
    }

    @Test
    @DisplayName("write(position, CharSequence) validates offset and length bounds")
    void shouldValidateCharSequenceBounds() {
        VanillaBytes<?> vanilla = (VanillaBytes<?>) bytes;

        assertThrows(IllegalArgumentException.class,
                () -> vanilla.write(0, "Hello", 0, 100),
                "write(position, CharSequence) should reject invalid bounds");
    }

    // ========== readVolatileLong ==========

    @Test
    @DisplayName("readVolatileLong reads with volatile memory barrier semantics")
    void shouldReadVolatileLong() {
        bytes.writeLong(0, 0x123456789ABCDEF0L);

        long result = bytes.readVolatileLong(0);

        assertEquals(0x123456789ABCDEF0L, result,
                "readVolatileLong should return the stored long value");
    }

    // ========== Heap Bytes Tests ==========

    @Test
    @DisplayName("read and write operations work on heap-backed bytes")
    void shouldWorkOnHeapBytes() {
        Bytes<?> heapBytes = Bytes.allocateElasticOnHeap(256);
        try {
            heapBytes.append8bit("Heap test");

            heapBytes.readPosition(0);
            StringBuilder sb = new StringBuilder();
            while (heapBytes.readRemaining() > 0) {
                sb.append((char) heapBytes.readByte());
            }
            assertEquals("Heap test", sb.toString(),
                    "operations should work on heap-backed bytes");
        } finally {
            heapBytes.releaseLast();
        }
    }

    @Test
    @DisplayName("isEqual compares correctly on heap-backed bytes")
    void shouldCompareOnHeapBytes() {
        Bytes<?> heapBytes = Bytes.allocateElasticOnHeap(256);
        try {
            heapBytes.append8bit("Hello");

            assertTrue(heapBytes.isEqual("Hello"),
                    "isEqual should compare correctly on heap-backed bytes");
        } finally {
            heapBytes.releaseLast();
        }
    }

    // ========== Comparable Interface ==========

    @Test
    @DisplayName("compareTo orders CharSequence values lexicographically by ASCII order")
    void shouldCompareLexicographically() {
        bytes.append8bit("Hello");

        @SuppressWarnings("unchecked")
        Comparable<CharSequence> comparable = (Comparable<CharSequence>) bytes;

        assertTrue(comparable.compareTo("Hallo") > 0,
                "compareTo should order Hello after Hallo");
        assertTrue(comparable.compareTo("Hello") == 0,
                "compareTo should treat identical strings as equal");
        assertTrue(comparable.compareTo("World") < 0,
                "compareTo should order Hello before World");
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("append8bit handles empty string without advancing position")
    void shouldHandleEmptyStringAppend() {
        bytes.append8bit("");

        assertEquals(0, bytes.writePosition(),
                "append8bit should not advance position for empty string");
    }

    @Test
    @DisplayName("append8bit handles large string input length 1000")
    void shouldHandleLargeStringAppend() {
        char[] chars = new char[1000];
        Arrays.fill(chars,'A');
        String longString = new String(chars);
        bytes.append8bit(longString);

        assertEquals(1000, bytes.writePosition(),
                "append8bit should write 1000 bytes for 1000 characters");
    }

    @ParameterizedTest(name = "append8bit writes {0} bytes for input length")
    @ValueSource(ints = {1, 10, 100, 255})
    @DisplayName("append8bit handles several input lengths without truncation")
    void shouldAppend8bitVariousLengths(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars,'A');
        String str = new String(chars);
        bytes.append8bit(str);

        assertEquals(length, bytes.writePosition(),
                "append8bit should write " + length + " bytes for input length");
    }

    // ========== Mixed Operations ==========

    @Test
    @DisplayName("mixed read and write operations preserve data order")
    void shouldHandleMixedOperations() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.append8bit("Test");
        bytes.writeInt(42);

        bytes.readPosition(0);
        assertEquals(0x123456789ABCDEF0L, bytes.readLong(),
                "readLong should return the original long value");

        byte[] strBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            strBytes[i] = bytes.readByte();
        }
        assertEquals("Test", new String(strBytes),
                "readByte sequence should decode Test correctly");

        assertEquals(42, bytes.readInt(),
                "readInt should return the original int value");
    }

    @Test
    @DisplayName("clear and reuse preserves new content after reset")
    void shouldClearAndReuse() {
        bytes.writeLong(0x123456789ABCDEF0L);
        bytes.clear();
        bytes.append8bit("Reused");

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        while (bytes.readRemaining() > 0) {
            sb.append((char) bytes.readByte());
        }
        assertEquals("Reused", sb.toString(),
                "bytes should be reusable after clear");
    }
}
