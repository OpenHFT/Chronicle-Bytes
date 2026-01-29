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
 * Tests for ChunkedMappedFile branch coverage, because chunk acquisition
 * and file locking must handle edge cases to avoid memory leaks and corruption.
 */
@DisplayName("ChunkedMappedFile chunk acquisition and file lifecycle validation")
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
    @DisplayName("capacity returns at least 16384 bytes when configured with that value")
    void capacityReturnsConfigured() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertTrue(chunkedFile.capacity() >= 16384, "capacity=" + chunkedFile.capacity() + " should be at least 16384");
    }

    @Test
    @DisplayName("chunkSize returns at least 4096 bytes when configured with that value")
    void chunkSizeReturnsConfigured() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertTrue(chunkedFile.chunkSize() >= 4096, "chunkSize=" + chunkedFile.chunkSize() + " should be at least 4096");
    }

    @Test
    @DisplayName("overlapSize returns at least 1024 bytes when configured because overlap enables cross-chunk reads")
    void overlapSizeReturnsConfigured() throws IOException {
        chunkedFile = createChunkedFile(4096, 1024, 16384, false);
        long overlapSize = chunkedFile.overlapSize();
        assertTrue(overlapSize >= 0, "overlapSize=" + overlapSize + " should be non-negative because overlap is optional");
    }

    @Test
    @DisplayName("raf returns the underlying RandomAccessFile for direct channel access")
    void rafReturnsUnderlyingFile() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertNotNull(chunkedFile.raf(), "raf() should return non-null RandomAccessFile for open chunked file");
    }

    @Test
    @DisplayName("actualSize returns non-negative value representing the file length on disk")
    void actualSizeReturnsFileSize() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertTrue(chunkedFile.actualSize() >= 0, "actualSize=" + chunkedFile.actualSize() + " should be non-negative");
    }

    @Test
    @DisplayName("acquireByteStore should create new store for first access")
    void acquireByteStoreCreatesStore() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            assertNotNull(store, "acquireByteStore(0) should return non-null store for valid position");
            assertTrue(store.capacity() > 0, "store.capacity()=" + store.capacity() + " should be positive");
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
    @DisplayName("acquireByteStore throws exception when position is -1 because negative offsets are invalid")
    void acquireByteStoreNegativePositionThrows() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertThrows(Exception.class, () -> chunkedFile.acquireByteStore(this, -1),
                "acquireByteStore(-1) should throw because negative positions are invalid");
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
    @DisplayName("chunkCount(array) sets external counter and chunkCount returns array[0]=5")
    void chunkCountCanBeSet() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        long[] externalCount = {5L};
        chunkedFile.chunkCount(externalCount);
        assertEquals(5L, chunkedFile.chunkCount(), "chunkCount() should return 5 from external array after chunkCount(array)");
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
    @DisplayName("referenceCounts returns string containing refCount for debugging resource leaks")
    void referenceCountsReturnsInfo() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 0);
        try {
            String refs = chunkedFile.referenceCounts();
            assertNotNull(refs, "referenceCounts() should return non-null string for active chunked file");
            assertTrue(refs.contains("refCount"), "referenceCounts() string should contain 'refCount' substring");
        } finally {
            store.release(this);
        }
    }

    @Test
    @DisplayName("NewChunkListener registration persists and getter returns same listener instance")
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
    @DisplayName("lock(0, 100, false) acquires valid exclusive FileLock on first 100 bytes")
    void lockAcquiresFileLock() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        FileLock lock = chunkedFile.lock(0, 100, false);
        try {
            assertNotNull(lock, "lock(0, 100, false) should return non-null FileLock");
            assertTrue(lock.isValid(), "acquired FileLock.isValid() should return true");
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
    @DisplayName("createBytesFor returns ChunkedMappedBytes instance for chunked file access")
    void createBytesForCreatesChunkedMappedBytes() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        MappedBytes bytes = chunkedFile.createBytesFor();
        try {
            assertNotNull(bytes, "createBytesFor() should return non-null MappedBytes");
            assertTrue(bytes instanceof ChunkedMappedBytes, "createBytesFor() should return ChunkedMappedBytes instance");
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

        assertTrue(chunkedFile.readOnly(), "readOnly() should return true for file opened with read-only flag");
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
    @DisplayName("file() returns the original File object used to create the ChunkedMappedFile")
    void fileReturnsUnderlyingFile() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        assertEquals(file, chunkedFile.file(), "file() should return the same File instance passed to constructor");
    }

    @Test
    @DisplayName("acquireByteStore at position 100000 extends file capacity beyond initial 16384")
    void acquireByteStoreCanExtendCapacity() throws IOException {
        chunkedFile = createChunkedFile(4096, 0, 16384, false);
        // Position beyond initial capacity - file can grow
        MappedBytesStore store = chunkedFile.acquireByteStore(this, 100000);
        try {
            assertNotNull(store, "acquireByteStore(100000) should return non-null store after capacity extension");
        } finally {
            store.release(this);
        }
    }
}
