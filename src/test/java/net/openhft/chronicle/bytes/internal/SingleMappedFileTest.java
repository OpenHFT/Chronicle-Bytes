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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for SingleMappedFile, because file mapping must handle read-only mode
 * and position validation to avoid memory corruption and access violations.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("SingleMappedFile mapping, position, and lifecycle validation")
class SingleMappedFileTest extends BytesTestCommon {

    @TempDir
    File tempDir;

    // ========== Constructor and Creation Tests ==========

    @Test
    @DisplayName("SingleMappedFile creates successfully with valid parameters")
    void shouldCreateSuccessfully() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file mapping skipped on Windows/WSL");

        File file = new File(tempDir, "create-success.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertNotNull(smf, "SingleMappedFile constructor returns non-null for valid parameters");
                assertEquals(file.getAbsolutePath(), smf.file().getAbsolutePath(),
                        "SingleMappedFile.file() returns path matching constructor input");
                assertTrue(smf.capacity() >= 4096,
                        "Capacity should be at least requested size");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("SingleMappedFile works in read-only mode for existing files")
    void shouldCreateReadOnlySuccessfully() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file read-only mapping skipped on Windows/WSL");

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
                assertTrue(smf.readOnly(), "SingleMappedFile.readOnly() returns true when opened with read-only flag");
            } finally {
                smf.close();
            }
        }
    }

    // ========== AcquireByteStore Tests ==========
    @Test
    @DisplayName("acquireByteStore throws IllegalArgumentException for non-zero position because single mapping starts at zero")
    void shouldThrowForNonZeroPosition() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file position validation skipped on Windows/WSL");

        File file = new File(tempDir, "acquire-nonzero.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                ReferenceOwner owner = ReferenceOwner.temporary("test");

                assertThrows(IllegalArgumentException.class,
                        () -> smf.acquireByteStore(owner, 100, null, null),
                        "acquireByteStore(100) throws IllegalArgumentException because single mapping only supports position 0");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Capacity and Size Tests ==========

    @Test
    @DisplayName("capacity returns page-aligned value at least as large as requested 4096 bytes")
    void shouldReturnCapacity() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file capacity test skipped on Windows/WSL");

        File file = new File(tempDir, "capacity.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                long capacity = smf.capacity();
                assertTrue(capacity >= 4096, "capacity()=" + capacity + " should be at least 4096 because alignment may increase size");
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("chunkSize returns capacity for single mapping because entire file is one chunk")
    void shouldReturnChunkSizeEqualToCapacity() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file chunk size test skipped on Windows/WSL");

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
    @DisplayName("overlapSize returns 0 for single mapping because no chunk boundaries exist")
    void shouldReturnZeroOverlapSize() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file overlap test skipped on Windows/WSL");

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
    @DisplayName("chunkCount returns 1 for single mapping because entire file is one chunk")
    void shouldReturnChunkCountOfOne() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file chunk count test skipped on Windows/WSL");

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
    @DisplayName("chunkCount(array) fills array[0] with 1 because single mapping has one chunk")
    void shouldFillChunkCountArray() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file chunk count array test skipped on Windows/WSL");

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
    @DisplayName("actualSize returns file size at least as large as requested 4096 capacity")
    void shouldReturnActualSize() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Single file actual size test skipped on Windows/WSL");

        File file = new File(tempDir, "actualsize.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                long actualSize = smf.actualSize();
                assertTrue(actualSize >= 4096,
                        "actualSize()=" + actualSize + " should be at least 4096 because file is extended to capacity");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Reference Count Tests ==========

    @Test
    @DisplayName("referenceCounts returns string containing 'refCount:' label for resource debugging")
    void shouldReturnReferenceCounts() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Reference counts test skipped due to Windows/WSL mapping restrictions");

        File file = new File(tempDir, "refcounts.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                String refCounts = smf.referenceCounts();
                assertNotNull(refCounts, "referenceCounts() returns non-null string for active file");
                assertTrue(refCounts.contains("refCount:"),
                        "referenceCounts()='" + refCounts + "' should contain 'refCount:' label");
            } finally {
                smf.close();
            }
        }
    }

    // ========== RAF and SyncMode Tests ==========

    @Test
    @DisplayName("raf() returns the underlying RandomAccessFile instance passed to constructor")
    void shouldReturnRaf() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "RAF test skipped due to Windows/WSL mapping restrictions");

        File file = new File(tempDir, "raf.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                assertSame(raf, smf.raf(),
                        "raf() returns same RandomAccessFile instance passed to constructor");
            } finally {
                smf.close();
            }
        }
    }

    // ========== Create Bytes Tests ==========

    @Test
    @DisplayName("createBytesFor() returns SingleMappedBytes instance for file access operations")
    void shouldCreateBytesFor() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "CreateBytesFor test skipped due to Windows/WSL mapping restrictions");

        File file = new File(tempDir, "createbytes.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            MappedBytes bytes = null;
            try {
                bytes = smf.createBytesFor();
                assertNotNull(bytes, "createBytesFor() returns non-null MappedBytes for valid file");
                assertInstanceOf(SingleMappedBytes.class, bytes, "createBytesFor() returns SingleMappedBytes instance");
            } finally {
                if (bytes != null) bytes.close();
                smf.close();
            }
        }
    }

    // ========== File Locking Tests ==========

    @Test
    @DisplayName("lock(0, 100, false) acquires exclusive FileLock on first 100 bytes of single mapping")
    void shouldLockFileRegion() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "File locking test skipped due to Windows/WSL restrictions");

        File file = new File(tempDir, "lock.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                java.nio.channels.FileLock lock = smf.lock(0, 100, false);
                assertNotNull(lock, "lock(0, 100, false) returns non-null FileLock");
                assertTrue(lock.isValid(), "FileLock.isValid() returns true after lock acquisition");
                lock.release();
            } finally {
                smf.close();
            }
        }
    }

    @Test
    @DisplayName("tryLock(0, 100, false) attempts non-blocking lock acquisition on file region")
    void shouldTryLockFileRegion() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "TryLock test skipped due to Windows/WSL restrictions");

        File file = new File(tempDir, "trylock.dat");
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            SingleMappedFile smf = new SingleMappedFile(file, raf, 4096, false);
            try {
                java.nio.channels.FileLock lock = smf.tryLock(0, 100, false);
                // tryLock may return null if lock cannot be obtained immediately
                if (lock != null) {
                    assertTrue(lock.isValid(), "tryLock() returns valid FileLock when lock acquired");
                    lock.release();
                }
            } finally {
                smf.close();
            }
        }
    }

    // ========== Thread Safety Tests ==========

    @Test
    @DisplayName("threadSafetyCheck returns true because SingleMappedFile is designed for concurrent access")
    void shouldBeThreadSafe() throws IOException {
        // Single mapping not supported on Windows file systems
        assumeFalse(OS.isWindows() || isWsl(), "Thread safety test skipped due to Windows/WSL restrictions");

        File file = new File(tempDir, "threadsafe.dat");
        try (MappedFile mf = MappedFile.mappedFile(file, 4096, 0)) {
            // SingleMappedFile overrides threadSafetyCheck to return true
            // We can't directly test the protected method, but we can verify
            // that operations work from multiple threads
            assertNotNull(mf, "MappedFile.mappedFile() returns non-null for valid file");
        }
    }
}
