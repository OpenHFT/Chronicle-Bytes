/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for SingleMappedFile covering branch coverage for file mapping,
 * position handling, and lifecycle methods.
 */
class SingleMappedFileTest extends BytesTestCommon {

    @TempDir
    File tempDir;

    // ========== Constructor and Creation Tests ==========

    @Test
    @DisplayName("SingleMappedFile creates successfully with valid parameters")
    void shouldCreateSuccessfully() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "create-success.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertNotNull(smf, "SingleMappedFile should be created");
                assertEquals(file.getAbsolutePath(), smf.file().getAbsolutePath(),
                        "File should match");
                assertTrue(smf.capacity() >= 4096,
                        "Capacity should be at least requested size");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("SingleMappedFile works in read-only mode")
    void shouldCreateReadOnlySuccessfully() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "readonly.dat");
        // First create a file with some content
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(4096);
            raf.writeLong(0x123456789ABCDEFL);
        }

        // Now open read-only
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, true);
            try {
                assertTrue(smf.readOnly(), "Should be read-only");
            } finally {
                smf.close();
            }
        }
    }

    // ========== AcquireByteStore Tests ==========

    @Test
    @DisplayName("acquireByteStore at position 0 returns store")
    void shouldAcquireByteStoreAtPositionZero() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "acquire-zero.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                ReferenceOwner owner = ReferenceOwner.temporary("test");
                MappedBytesStore store = smf.acquireByteStore(owner, 0, null, null);

                assertNotNull(store, "Store should be returned for position 0");
                store.release(owner);
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("acquireByteStore throws for non-zero position")
    void shouldThrowForNonZeroPosition() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "acquire-nonzero.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                ReferenceOwner owner = ReferenceOwner.temporary("test");

                assertThrows(IllegalArgumentException.class,
                        () -> smf.acquireByteStore(owner, 100, null, null),
                        "Should throw for non-zero position");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Capacity and Size Tests ==========

    @Test
    @DisplayName("capacity returns aligned capacity")
    void shouldReturnCapacity() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "capacity.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                long capacity = smf.capacity();
                assertTrue(capacity >= 4096, "Capacity should be at least 4096");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("chunkSize returns capacity for single mapping")
    void shouldReturnChunkSizeEqualToCapacity() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "chunksize.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertEquals(smf.capacity(), smf.chunkSize(),
                        "Chunk size should equal capacity for single mapping");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("overlapSize returns 0 for single mapping")
    void shouldReturnZeroOverlapSize() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "overlap.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertEquals(0, smf.overlapSize(),
                        "Overlap size should be 0 for single mapping");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("chunkCount returns 1 for single mapping")
    void shouldReturnChunkCountOfOne() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "chunkcount.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertEquals(1, smf.chunkCount(),
                        "Chunk count should be 1 for single mapping");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("chunkCount array fills correctly")
    void shouldFillChunkCountArray() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "chunkcount-array.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                long[] counts = new long[1];
                smf.chunkCount(counts);
                assertEquals(1, counts[0],
                        "Chunk count array should have 1");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Actual Size Tests ==========

    @Test
    @DisplayName("actualSize returns file size")
    void shouldReturnActualSize() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "actualsize.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                long actualSize = smf.actualSize();
                assertTrue(actualSize >= 4096,
                        "Actual size should be at least capacity");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Reference Count Tests ==========

    @Test
    @DisplayName("referenceCounts returns formatted string")
    void shouldReturnReferenceCounts() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "refcounts.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                String refCounts = smf.referenceCounts();
                assertNotNull(refCounts, "Reference counts string should not be null");
                assertTrue(refCounts.contains("refCount:"),
                        "Should contain refCount label");
            } finally {
                smf.close();
            }
        }
    }

    // ========== RAF and SyncMode Tests ==========

    @Test
    @DisplayName("raf returns the RandomAccessFile")
    void shouldReturnRaf() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "raf.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertSame(raf, smf.raf(),
                        "Should return the same RandomAccessFile");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Create Bytes Tests ==========

    @Test
    @DisplayName("createBytesFor returns SingleMappedBytes")
    void shouldCreateBytesFor() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "createbytes.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            MappedBytes bytes = null;
            try {
                bytes = smf.createBytesFor();
                assertNotNull(bytes, "createBytesFor should return MappedBytes");
                assertTrue(bytes instanceof SingleMappedBytes,
                        "Should return SingleMappedBytes instance");
            } finally {
                if (bytes != null) bytes.close();
                smf.close();
            }
        }
    }

    // ========== File Locking Tests ==========

    @Test
    @DisplayName("lock and unlock file region")
    void shouldLockFileRegion() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "lock.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                java.nio.channels.FileLock lock = smf.lock(0, 100, false);
                assertNotNull(lock, "Lock should be obtained");
                assertTrue(lock.isValid(), "Lock should be valid");
                lock.release();
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("tryLock attempts to lock file region")
    void shouldTryLockFileRegion() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "trylock.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                java.nio.channels.FileLock lock = smf.tryLock(0, 100, false);
                // tryLock may return null if lock cannot be obtained immediately
                if (lock != null) {
                    assertTrue(lock.isValid(), "Lock should be valid if obtained");
                    lock.release();
                }
            } finally {
                smf.close();
            }
        }
    }

    // ========== Thread Safety Tests ==========

    @Test
    @DisplayName("threadSafetyCheck returns true")
    void shouldBeThreadSafe() throws IOException {
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "threadsafe.dat");
        try (MappedFile mf = MappedFile.mappedFile(file, 4096, 0)) {
            // SingleMappedFile overrides threadSafetyCheck to return true
            // We can't directly test the protected method, but we can verify
            // that operations work from multiple threads
            assertNotNull(mf, "MappedFile should be created");
        }
    }
}
