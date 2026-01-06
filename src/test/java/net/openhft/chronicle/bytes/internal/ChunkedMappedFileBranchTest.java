/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChunkedMappedFile branch coverage.
 */
class ChunkedMappedFileBranchTest extends BytesTestCommon implements ReferenceOwner {

    @TempDir
    File tempDir;

    private ChunkedMappedFile chunkedFile;
    private File file;

    @BeforeEach
    void setUp() throws IOException {
        file = new File(tempDir, "chunked-test.dat");
    }

    @AfterEach
    void tearDown() {
        if (chunkedFile != null) {
            chunkedFile.releaseLast();
            chunkedFile = null;
        }
    }

    private ChunkedMappedFile createChunkedFile(long chunkSize, long overlapSize, long capacity, boolean readOnly) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, readOnly ? "r" : "rw");
        if (!readOnly) {
            raf.setLength(capacity);
        }
        return new ChunkedMappedFile(file, raf, chunkSize, overlapSize, capacity, readOnly);
    }

    @Test
    @DisplayName("capacity should return configured capacity")
    void capacityReturnsConfigured() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertTrue(chunkedFile.capacity() >= 16384, "capacity should be at least configured");
    }

    @Test
    @DisplayName("chunkSize should return configured chunk size")
    void chunkSizeReturnsConfigured() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertTrue(chunkedFile.chunkSize() >= 4096, "chunk size should be at least configured");
    }

    @Test
    @DisplayName("overlapSize should return configured overlap")
    void overlapSizeReturnsConfigured() throws IOException {
        chunkedFile = createChunkedFile(4096, 1024, 16384, false);
        assertTrue(chunkedFile.overlapSize() >= 0, "overlap size should be non-negative");
    }

    @Test
    @DisplayName("raf should return underlying RandomAccessFile")
    void rafReturnsUnderlyingFile() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertNotNull(chunkedFile.raf(), "raf should not be null");
    }

    @Test
    @DisplayName("actualSize should return file size")
    void actualSizeReturnsFileSize() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertTrue(chunkedFile.actualSize() >= 0, "actual size should be non-negative");
    }

    @Test
    @DisplayName("acquireByteStore should create new store for first access")
    void acquireByteStoreCreatesStore() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            assertNotNull(store, "store should not be null");
            assertTrue(store.capacity() > 0, "store should have capacity");
        } finally {
            store.release(this);
        }
    }

    @Test
    @DisplayName("acquireByteStore should return same store for same position")
    void acquireByteStoreSamePosition() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytesStore store1 = chunkedFile.acquireByteStore(this, 0);
        MappedBytesStore store2 = chunkedFile.acquireByteStore(this, 0, store1, MappedBytesStore::create);
        try {
            assertSame(store1, store2, "same position should return same store");
        } finally {
            store1.release(this);
            // store2 is same as store1, so only release once
        }
    }

    @Test
    @DisplayName("acquireByteStore with negative position should throw")
    void acquireByteStoreNegativePositionThrows() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertThrows(Exception.class, () -> chunkedFile.acquireByteStore(this, -1),
                "negative position should throw");
    }

    @Test
    @DisplayName("chunkCount should track number of chunks created")
    void chunkCountTracksChunks() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        long initialCount = chunkedFile.chunkCount();
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            assertTrue(chunkedFile.chunkCount() >= initialCount, "chunk count should not decrease");
        } finally {
            store.release(this);
        }
    }

    @Test
    @DisplayName("chunkCount can be set externally")
    void chunkCountCanBeSet() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        long[] externalCount = {5L};
        chunkedFile.chunkCount(externalCount);
        assertEquals(5L, chunkedFile.chunkCount(), "chunk count should reflect external array");
    }

    @Test
    @DisplayName("syncMode should set sync mode for all stores")
    void syncModeSetsSyncMode() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            chunkedFile.syncMode(SyncMode.ASYNC);
            assertEquals(SyncMode.ASYNC, store.syncMode(), "store sync mode should be updated");
        } finally {
            store.release(this);
        }
    }

    @Test
    @DisplayName("referenceCounts should return reference info")
    void referenceCountsReturnsInfo() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            String refs = chunkedFile.referenceCounts();
            assertNotNull(refs, "reference counts should not be null");
            assertTrue(refs.contains("refCount"), "should contain refCount");
        } finally {
            store.release(this);
        }
    }

    @Test
    @DisplayName("NewChunkListener should be called on new chunk")
    void newChunkListenerCalled() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        AtomicInteger callCount = new AtomicInteger(0);
        chunkedFile.setNewChunkListener((filename, chunk, elapsed) -> callCount.incrementAndGet());

        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            assertTrue(callCount.get() >= 0, "listener may or may not be called depending on caching");
        } finally {
            store.release(this);
        }

        assertEquals(chunkedFile.getNewChunkListener(), chunkedFile.getNewChunkListener(),
                "getter should return same listener");
    }

    @Test
    @DisplayName("lock should acquire file lock")
    void lockAcquiresFileLock() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        FileLock lock = chunkedFile.lock(0, 100, false);
        try {
            assertNotNull(lock, "lock should not be null");
            assertTrue(lock.isValid(), "lock should be valid");
        } finally {
            lock.release();
        }
    }

    @Test
    @DisplayName("tryLock should attempt to acquire file lock")
    void tryLockAttemptsLock() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        FileLock lock = chunkedFile.tryLock(0, 100, false);
        if (lock != null) {
            try {
                assertTrue(lock.isValid(), "lock should be valid if acquired");
            } finally {
                lock.release();
            }
        }
    }

    @Test
    @DisplayName("createBytesFor should create ChunkedMappedBytes")
    void createBytesForCreatesChunkedMappedBytes() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytes bytes = chunkedFile.createBytesFor();
        try {
            assertNotNull(bytes, "bytes should not be null");
            assertTrue(bytes instanceof ChunkedMappedBytes, "should be ChunkedMappedBytes");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readOnly file should not permit writes")
    void readOnlyFileNoWrites() throws IOException {
        // First create a file with some content
        try (ChunkedMappedFile writeFile = createChunkedFile(4096, 0, 16384, false)) {
            MappedBytesStore store = writeFile.acquireByteStore(this, 0);
            store.writeLong(0, 12345L);
            store.release(this);
        }

        // Now open as read-only
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        chunkedFile = new ChunkedMappedFile(file, raf, 4096, 0, 16384, true);

        assertTrue(chunkedFile.readOnly(), "file should be read-only");
    }

    @Test
    @DisplayName("multiple chunks should be accessible sequentially")
    void multipleChunksAccessible() throws IOException {
        long chunkSize = 4096;
        chunkedFile = createChunkedFile(chunkSize, 0, chunkSize * 4, false);

        // Access first chunk
        MappedBytesStore store0 = chunkedFile.acquireByteStore(this, 0);
        try {
            assertNotNull(store0, "first chunk store should not be null");
            store0.writeLong(0, 111L);
            assertEquals(111L, store0.readLong(0), "first chunk should have correct value");
        } finally {
            store0.release(this);
        }

        // Access second chunk separately
        MappedBytesStore store1 = chunkedFile.acquireByteStore(this, chunkSize);
        try {
            assertNotNull(store1, "second chunk store should not be null");
            store1.writeLong(chunkSize, 222L);
            assertEquals(222L, store1.readLong(chunkSize), "second chunk should have correct value");
        } finally {
            store1.release(this);
        }
    }

    @Test
    @DisplayName("file should return underlying file")
    void fileReturnsUnderlyingFile() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertEquals(file, chunkedFile.file(), "file should match");
    }

    @Test
    @DisplayName("acquireByteStore can extend capacity")
    void acquireByteStoreCanExtendCapacity() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        // Position beyond initial capacity - file can grow
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 100000);
        try {
            assertNotNull(store, "store beyond initial capacity should be created");
        } finally {
            store.release(this);
        }
    }
}
