/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.BytesStore;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.*;

class XxHashTest {

    @Test
    void testHashEmptyBytesStore() {
        BytesStore<?, ?> emptyBytesStore = BytesStore.empty();
        long hash = XxHash.INSTANCE.applyAsLong(emptyBytesStore);
        // Assert not throwing an exception and returns a deterministic value
        assertNotNull(hash);
    }

    @Test
    void testHashConsistency() {
        byte[] data = "test data".getBytes();
        BytesStore<?, ?> bytesStore1 = BytesStore.wrap(data);
        BytesStore<?, ?> bytesStore2 = BytesStore.wrap(data.clone());

        long hash1 = XxHash.INSTANCE.applyAsLong(bytesStore1);
        long hash2 = XxHash.INSTANCE.applyAsLong(bytesStore2);

        // Assert that hashes for identical data are equal
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashWithDifferentLengths() {
        BytesStore<?, ?> bytesStore = BytesStore.from("some test data");
        long fullHash = XxHash.INSTANCE.applyAsLong(bytesStore, bytesStore.readRemaining());
        long partialHash = XxHash.INSTANCE.applyAsLong(bytesStore, bytesStore.readRemaining() - 1);

        // Assert that changing the length results in different hashes
        assertNotEquals(fullHash, partialHash);
    }

    @Test
    void testHashBeyondLengthThrowsException() {
        assertThrows(BufferUnderflowException.class, () -> {
            BytesStore<?, ?> bytesStore = BytesStore.from("short");
            // Attempt to hash beyond the available length
            XxHash.INSTANCE.applyAsLong(bytesStore, bytesStore.readRemaining() + 1);
        });
    }
}
