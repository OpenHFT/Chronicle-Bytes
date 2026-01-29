/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.CommonMappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests mapped bytes boundary operations because correct handling of chunk
 * boundaries is critical to avoid data corruption when writes span multiple
 * memory-mapped regions.
 */
@DisplayName("Mapped bytes boundary conditions for write and read")
public class MappedBytesBoundaryTest extends BytesTestCommon {
    @BeforeEach
    public void setUp() {
        if (OS.isWindows())
            ignoreException("Unable to delete");
    }

    @Test
    @DisplayName("write across chunk boundary preserves bytes")
    public void writeAcrossChunkBoundary() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for boundary write test");

        final int chunk = 4096;
        final byte[] prefix = new byte[chunk - 4];
        final byte[] tail = "HELLO".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] expected = new byte[prefix.length + tail.length];
        // Build expected array: prefix bytes followed by tail bytes
        System.arraycopy(prefix, 0, expected, 0, prefix.length);
        // Append tail after prefix to form complete expected payload
        System.arraycopy(tail, 0, expected, prefix.length, tail.length);

        File file = new File(OS.getTarget(), "mapped-boundary-" + System.nanoTime() + ".dat");
        try (MappedBytes mb = MappedBytes.mappedBytes(file, chunk)) {
            // position at end of first chunk minus 4
            mb.writePosition(prefix.length);
            mb.write(tail);

            // read back from start
            mb.readPosition(0);
            byte[] actual = new byte[expected.length];
            mb.read(actual);
            assertArrayEquals(expected, actual,
                    "Chunk boundary write reads back expected bytes");
        }
        deleteIfPossible(file);
    }

    @Test
    @DisplayName("read only mapping rejects writes and reports flag")
    public void readOnlyMappingRejectsWritesAndReportsFlag() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for read only mapping test");

        File file = new File(OS.getTarget(), "mapped-readonly-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(OS.pageSize());
        }

        try (MappedBytes writable = MappedBytes.singleMappedBytes(file, OS.pageSize())) {
            writable.writeSkip(32);
            assertEquals(32, writable.writePosition(),
                    "Write position advances after skip");
        }

        try (MappedBytes readOnly = MappedBytes.singleMappedBytes(file, OS.pageSize(), true)) {
            assertTrue(((CommonMappedBytes) readOnly).isBackingFileReadOnly(),
                    "Backing file is marked read only");
            boolean writeFailed = false;
            try {
                readOnly.writeByte((byte) 0x7F);
            } catch (ReadOnlyBufferException | BufferOverflowException | IllegalStateException expected) {
                writeFailed = true;
            }
            assertTrue(writeFailed,
                    "Write to read-only mapping should fail");
        }

        deleteIfPossible(file);
    }

    @Test
    @DisplayName("writeSkip reserves space like queue writers")
    public void writeSkipReservesSpaceLikeQueueWriters() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for writeSkip test");

        File file = new File(OS.getTarget(), "mapped-write-skip-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
            bytes.writeSkip(1024);
            assertEquals(1024, bytes.writePosition(),
                    "writeSkip advances write position to reserved space");

            bytes.writeByte((byte) 0x5A);
            bytes.readPosition(1024);
            assertEquals((byte) 0x5A, bytes.readByte(),
                    "readByte returns value written after skip");
        } finally {
            deleteIfPossible(file);
        }
    }

    @Test
    @DisplayName("write8bit uses optimised path for ASCII strings")
    public void write8bitUsesOptimisedPathForAsciiStrings() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for write8bit test");

        File file = new File(OS.getTarget(), "mapped-write8bit-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        String message = "OrderAccepted";
        try {
            try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
                bytes.writePosition(0);
                bytes.write8bit(message);

                bytes.readPosition(0);
                assertEquals(message, bytes.read8bit(),
                        "read8bit returns expected ASCII message");
                assertTrue(bytes.writePosition() > message.length(),
                        "Write position advances past written payload");
            }
        } finally {
            deleteIfPossible(file);
        }
    }
}
