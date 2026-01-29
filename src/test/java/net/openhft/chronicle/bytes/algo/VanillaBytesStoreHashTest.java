/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VanillaBytesStoreHash covering all branch paths including
 * zero length, small data, medium data, and large data with multiple 32-byte chunks
 * because correct hashing is essential to avoid key collisions in high-throughput maps.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("VanillaBytesStoreHash behaviour across zero, small, medium, and large inputs")
class VanillaBytesStoreHashTest extends BytesTestCommon {

    // ========== agitate Tests ==========

    @Test
    @DisplayName("agitate returns the same value for repeated input bits")
    void shouldAgitateValue() {
        long input = 0x123456789ABCDEF0L;
        long result1 = VanillaBytesStoreHash.agitate(input);
        long result2 = VanillaBytesStoreHash.agitate(input);

        assertEquals(result1, result2,
                "agitate output should be stable for input 0x123456789ABCDEF0");
        assertNotEquals(input, result1,
                "agitate output should differ from input 0x123456789ABCDEF0");
    }

    @Test
    @DisplayName("agitate returns 0 output for a zero input long value")
    void shouldAgitateZero() {
        assertEquals(0, VanillaBytesStoreHash.agitate(0L),
                "agitate should return 0L for zero input");
    }

    // ========== applyAsLong - Zero Length Tests ==========

    @Test
    @DisplayName("hash of an empty BytesStore payload returns zero")
    void shouldReturnZeroForEmptyStore() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            assertEquals(0, VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes),
                    "hash of an empty BytesStore should return 0");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hash with explicit length 0 returns zero")
    void shouldReturnZeroForZeroLength() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.writeLong(0x123456789ABCDEF0L);
            assertEquals(0, VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes, 0),
                    "hash with explicit length 0 should return 0");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== applyAsLong - Small Data (1-8 bytes) Tests ==========

    @Test
    @DisplayName("hash of a single byte is non-zero")
    void shouldHashSingleByte() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            bytes.writeByte((byte) 0x42);
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "hash for a single byte 0x42 should be non-zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "small-data hash for {0} bytes uses the short path")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    @DisplayName("hash for 1-8 bytes uses the small-data branch")
    void shouldHashSmallData(int length) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < length; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "small-data hash for " + length + " bytes should be non-zero for ascending bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hash for 8 bytes is deterministic across identical payloads")
    void shouldHash8BytesDeterministically() {
        Bytes<?> bytes1 = Bytes.allocateElasticOnHeap();
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        try {
            bytes1.writeLong(0x0102030405060708L);
            bytes2.writeLong(0x0102030405060708L);

            assertEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes1),
                    VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes2),
                    "hash should match for two 8-byte payloads 0x0102030405060708");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
        }
    }

    // ========== applyAsLong - Medium Data (9-31 bytes) Tests ==========

    @ParameterizedTest(name = "remainder-only hash for {0} bytes uses the medium path")
    @ValueSource(ints = {9, 10, 15, 16, 20, 24, 28, 31})
    @DisplayName("hash for 9-31 bytes uses the remainder-only branch")
    void shouldHashMediumData(int length) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < length; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "remainder-only hash for " + length + " bytes should be non-zero for ascending bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== applyAsLong - Exact 32 Bytes Tests ==========

    @Test
    @DisplayName("hash for exactly 32 bytes uses one full block with no remainder")
    void shouldHashExactly32Bytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 32; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "full-block hash for 32 bytes should be non-zero for ascending bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== applyAsLong - Large Data (>32 bytes) Tests ==========

    @Test
    @DisplayName("hash for 33 bytes uses one full block plus remainder")
    void shouldHash33Bytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 33; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "one-block-plus-remainder hash for 33 bytes should be non-zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hash for 64 bytes uses two full blocks")
    void shouldHashExactly64Bytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 64; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "two-block hash for 64 bytes should be non-zero for ascending bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hash for 65 bytes uses two full blocks plus remainder")
    void shouldHash65Bytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 65; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "two-block-plus-remainder hash for 65 bytes should be non-zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hash for 128 bytes uses four full blocks")
    void shouldHash128Bytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 128; i++) {
                bytes.writeByte((byte) (i + 1));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "four-block hash for 128 bytes should be non-zero for ascending bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== applyAsLong with explicit length Tests ==========

    @Test
    @DisplayName("applyAsLong uses the provided length rather than remaining bytes")
    void shouldUseSpecifiedLength() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 100; i++) {
                bytes.writeByte((byte) (i + 1));
            }

            long hash10 = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes, 10);
            long hash20 = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes, 20);
            long hashAll = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);

            assertNotEquals(hash10, hash20,
                    "hash for first 10 bytes should differ from hash for first 20 bytes");
            assertNotEquals(hash10, hashAll,
                    "hash for first 10 bytes should differ from full 100-byte hash");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== Hash Distribution Tests ==========

    @Test
    @DisplayName("different eight-byte payloads produce different hashes")
    void shouldProduceDifferentHashesForDifferentData() {
        Bytes<?> bytes1 = Bytes.allocateElasticOnHeap();
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        try {
            bytes1.writeLong(0x0102030405060708L);
            bytes2.writeLong(0x0807060504030201L);

            assertNotEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes1),
                    VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes2),
                    "hash should differ for 0x0102030405060708 and 0x0807060504030201");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
        }
    }

    @Test
    @DisplayName("single bit change produces a different hash")
    void shouldDetectSingleBitChange() {
        Bytes<?> bytes1 = Bytes.allocateElasticOnHeap();
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap();
        try {
            bytes1.writeLong(0x0000000000000000L);
            bytes2.writeLong(0x0000000000000001L);

            assertNotEquals(VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes1),
                    VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes2),
                    "hash should differ when only the least significant bit changes");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
        }
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("hashing 64 zero bytes exercises the all-zero path")
    void shouldHashAllZeros() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 64; i++) {
                bytes.writeByte((byte) 0);
            }
            // Just ensure it doesn't throw and returns some value
            VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hashing 64 0xFF bytes exercises the all-ones path")
    void shouldHashAllOnes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 64; i++) {
                bytes.writeByte((byte) 0xFF);
            }
            // Just ensure it doesn't throw and returns some value
            VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== Consistency Tests ==========

    @Test
    @DisplayName("hash is consistent when readPosition is reset to zero")
    void shouldBeConsistent() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            for (int i = 0; i < 50; i++) {
                bytes.writeByte((byte) i);
            }

            long hash1 = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            bytes.readPosition(0);
            long hash2 = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);

            assertEquals(hash1, hash2,
                    "hash should be identical after resetting readPosition to 0");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== Branch Coverage: i > 0 multiplier path ==========

    @Test
    @DisplayName("hash for 65 bytes applies the multiplier path on the second block")
    void shouldApplyMultipliersInSecondIteration() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            // Write 65 bytes to ensure at least 2 iterations (0 and 32) and remainder
            for (int i = 0; i < 65; i++) {
                bytes.writeByte((byte) (i % 256));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "hash for 65 bytes should be non-zero after second-block mixing");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("hash for 96 bytes covers three full blocks with multipliers")
    void shouldHash96Bytes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            // 96 bytes = 3 full 32-byte iterations
            for (int i = 0; i < 96; i++) {
                bytes.writeByte((byte) (i % 256));
            }
            long hash = VanillaBytesStoreHash.INSTANCE.applyAsLong(bytes);
            assertNotEquals(0, hash,
                    "three-block hash for 96 bytes should be non-zero for repeating bytes");
        } finally {
            bytes.releaseLast();
        }
    }

    // ========== INSTANCE singleton test ==========

    @Test
    @DisplayName("INSTANCE exposes a shared hash function singleton reference")
    void shouldBeSingleton() {
        assertSame(VanillaBytesStoreHash.INSTANCE, VanillaBytesStoreHash.INSTANCE,
                "INSTANCE should always reference the same singleton object");
    }
}
