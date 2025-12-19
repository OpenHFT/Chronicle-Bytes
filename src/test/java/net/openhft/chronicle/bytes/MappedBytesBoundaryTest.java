/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.CommonMappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class MappedBytesBoundaryTest extends BytesTestCommon {
    @BeforeEach
    public void setUp() {
        if (OS.isWindows())
            ignoreException("Unable to delete");
    }

    @Test
    public void writeAcrossChunkBoundary() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final int chunk = 4096;
        final byte[] prefix = new byte[chunk - 4];
        final byte[] tail = "HELLO".getBytes(ISO_8859_1);
        final byte[] expected = new byte[prefix.length + tail.length];
        System.arraycopy(prefix, 0, expected, 0, prefix.length);
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
            assertArrayEquals(expected, actual, "write/read across chunk boundary should preserve all bytes");
        }
        deleteIfPossible(file);
    }

    @Test
    public void readOnlyMappingRejectsWritesAndReportsFlag() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File file = new File(OS.getTarget(), "mapped-readonly-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(OS.pageSize());
        }

        try (MappedBytes writable = MappedBytes.singleMappedBytes(file, OS.pageSize())) {
            writable.writeSkip(32);
            assertEquals(32, writable.writePosition(), "writeSkip(32) should advance write position to 32 in writable mapping");
        }

        try (MappedBytes readOnly = MappedBytes.singleMappedBytes(file, OS.pageSize(), true)) {
            assertTrue(readOnly.isBackingFileReadOnly(), "singleMappedBytes with readOnly=true should report backing file as read-only");
            boolean writeFailed = false;
            try {
                readOnly.writeByte((byte) 0x7F);
            } catch (ReadOnlyBufferException | BufferOverflowException | IllegalStateException expected) {
                writeFailed = true;
            }
            assertTrue(writeFailed, "Expected write to read-only mapping to fail");
        }

        deleteIfPossible(file);
    }

    @Test
    public void writeSkipReservesSpaceLikeQueueWriters() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File file = new File(OS.getTarget(), "mapped-write-skip-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
            bytes.writeSkip(1024);
            assertEquals(1024, bytes.writePosition(), "writeSkip(1024) should advance write position to 1024 for space reservation");

            bytes.writeByte((byte) 0x5A);
            bytes.readPosition(1024);
            assertEquals((byte) 0x5A, bytes.readByte(), "byte at position 1024 should be 0x5A after writeSkip and writeByte");
        } finally {
            deleteIfPossible(file);
        }
    }

    @Test
    public void write8bitUsesOptimisedPathForAsciiStrings() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File file = new File(OS.getTarget(), "mapped-write8bit-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        String message = "OrderAccepted";
        try {
            try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
                bytes.writePosition(0);
                bytes.write8bit(message);

                bytes.readPosition(0);
                assertEquals(message, bytes.read8bit(), "read8bit should return 'OrderAccepted' after write8bit in mapped bytes");
                assertTrue(bytes.writePosition() > message.length(), "Expected bytes to advance past written payload");
            }
        } finally {
            deleteIfPossible(file);
        }
    }
}
