/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.CommonMappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class MappedBytesBoundaryTest extends BytesTestCommon {

    @Test
    public void writeAcrossChunkBoundary() throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final int chunk = 4096;
        final byte[] prefix = new byte[chunk - 4];
        final byte[] tail = "HELLO".getBytes(StandardCharsets.ISO_8859_1);
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
            assertArrayEquals(expected, actual);
        }
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
            assertEquals(32, writable.writePosition());
        }

        try (MappedBytes readOnly = MappedBytes.singleMappedBytes(file, OS.pageSize(), true)) {
            assertTrue(((CommonMappedBytes) readOnly).isBackingFileReadOnly());
            boolean writeFailed = false;
            try {
                readOnly.writeByte((byte) 0x7F);
            } catch (ReadOnlyBufferException | BufferOverflowException | IllegalStateException expected) {
                writeFailed = true;
            }
            assertTrue("Expected write to read-only mapping to fail", writeFailed);
        }

        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void writeSkipReservesSpaceLikeQueueWriters() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File file = new File(OS.getTarget(), "mapped-write-skip-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
            bytes.writeSkip(1024);
            assertEquals(1024, bytes.writePosition());

            bytes.writeByte((byte) 0x5A);
            bytes.readPosition(1024);
            assertEquals((byte) 0x5A, bytes.readByte());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void write8bitUsesOptimisedPathForAsciiStrings() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        File file = new File(OS.getTarget(), "mapped-write8bit-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        String message = "OrderAccepted";
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
            bytes.writePosition(0);
            bytes.write8bit(message);

            bytes.readPosition(0);
            assertEquals(message, bytes.read8bit());
            assertTrue("Expected bytes to advance past written payload", bytes.writePosition() > message.length());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}
