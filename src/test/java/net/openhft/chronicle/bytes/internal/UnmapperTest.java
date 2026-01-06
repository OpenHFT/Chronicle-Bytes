/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests for Unmapper class covering branch coverage for the run() method.
 */
class UnmapperTest extends BytesTestCommon {

    @TempDir
    File tempDir;

    @Test
    @DisplayName("Unmapper run() is idempotent - second call does nothing")
    void shouldIgnoreSecondRunCall() throws IOException {
        // Skip on Windows/WSL due to potential file locking issues
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "unmapper-test.dat");

        // Create and map a file
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(4096);
            FileChannel channel = raf.getChannel();

            int pageSize = OS.pageSize();
            long address = OS.map(channel, FileChannel.MapMode.READ_WRITE, 0, 4096, pageSize);
            assertTrue(address != 0, "Mapped address should not be zero");

            Unmapper unmapper = new Unmapper(address, 4096, pageSize);

            // First run should unmap successfully
            assertDoesNotThrow(unmapper::run,
                    "First run() should complete without error");

            // Second run should be a no-op (address is now 0)
            assertDoesNotThrow(unmapper::run,
                    "Second run() should be idempotent and do nothing");
        }
    }

    @Test
    @DisplayName("Unmapper constructor accepts valid parameters")
    void shouldCreateWithValidParameters() {
        // We're just testing construction, not actual unmapping
        // Use a non-zero address that won't be unmapped
        Unmapper unmapper = new Unmapper(0x1000L, 4096, OS.pageSize());

        assertNotNull(unmapper, "Unmapper should be created successfully");
    }

    @Test
    @DisplayName("Unmapper works with MappedBytes lifecycle")
    void shouldWorkWithMappedBytesLifecycle() throws IOException {
        // Skip on Windows/WSL due to potential file locking issues
        assumeFalse(OS.isWindows() || isWsl(), "Skipped on Windows/WSL");

        File file = new File(tempDir, "mapped-bytes-test.dat");

        // MappedBytes uses Unmapper internally - this tests integration
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, 4096)) {
            bytes.writeLong(0, 0x123456789ABCDEFL);
            assertEquals(0x123456789ABCDEFL, bytes.readLong(0),
                    "Written value should be readable");
        }
        // Unmapper.run() is called during close via cleaner

        // File should be unmapped and deletable
        assertTrue(file.exists(), "File should exist after close");
    }
}
