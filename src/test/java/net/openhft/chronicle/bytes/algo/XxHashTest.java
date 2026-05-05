/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.BytesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validates xxHash implementations for Chronicle Bytes, including empty inputs,
 * small payloads, and behaviour when truncating hashes because consistent
 * deterministic hashing is required for data integrity and cache lookups.
 */
@DisplayName("XxHash - deterministic hashing for empty, short, and truncated inputs")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class XxHashTest {

    @Test
    @DisplayName("hash for empty bytes store is deterministic")
    public void testHashEmptyBytesStore() {
        BytesStore<?, ?> emptyBytesStore = BytesStore.empty();
        long hash = XxHash.INSTANCE.applyAsLong(emptyBytesStore);
        assertEquals(hash, XxHash.INSTANCE.applyAsLong(emptyBytesStore),
                "Empty bytes store hash remains stable across calls");
    }

    @Test
    @DisplayName("hash values match for identical inputs")
    public void testHashConsistency() {
        byte[] data = "test data".getBytes(StandardCharsets.ISO_8859_1);
        BytesStore<?, ?> bytesStore1 = BytesStore.wrap(data);
        BytesStore<?, ?> bytesStore2 = BytesStore.wrap(data.clone());

        long hash1 = XxHash.INSTANCE.applyAsLong(bytesStore1);
        long hash2 = XxHash.INSTANCE.applyAsLong(bytesStore2);

        // Assert that hashes for identical data are equal
        assertEquals(hash1, hash2,
                "Hash values match for identical byte arrays");
    }

    @Test
    @DisplayName("hash values differ when length changes")
    public void testHashWithDifferentLengths() {
        BytesStore<?, ?> bytesStore = BytesStore.from("some test data");
        long fullHash = XxHash.INSTANCE.applyAsLong(bytesStore, bytesStore.readRemaining());
        long partialHash = XxHash.INSTANCE.applyAsLong(bytesStore, bytesStore.readRemaining() - 1);

        // Assert that changing the length results in different hashes
        assertNotEquals(fullHash, partialHash,
                "Hash values differ when input length changes");
    }

    @Test
    @DisplayName("hashing beyond available length throws exception")
    public void testHashBeyondLengthThrowsException() {
        BytesStore<?, ?> bytesStore = BytesStore.from("short");
        // Attempt to hash beyond the available length
        assertThrows(BufferUnderflowException.class,
                () -> XxHash.INSTANCE.applyAsLong(bytesStore, bytesStore.readRemaining() + 1),
                "Hashing beyond available length throws underflow");
    }
}
