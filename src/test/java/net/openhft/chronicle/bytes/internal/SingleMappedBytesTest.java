/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("SingleMappedBytes covers write, read, and CAS branch scenarios")
public class SingleMappedBytesTest {

    @Test
    @DisplayName("write rejects array bounds when offset and length exceed data")
    public void writeRejectsArrayBounds() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes write bounds test");
        File file = newTempFile();
        byte[] data = new byte[4];
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            assertThrows(ArrayIndexOutOfBoundsException.class,
                    () -> bytes.write(0, data, 3, 2),
                    "Write should reject offset and length that exceed the array size");
        } finally {
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("write copies byte arrays into the mapping")
    public void writeCopiesByteArray() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes write copy test");
        File file = newTempFile();
        byte[] data = new byte[] {1, 2, 3, 4};
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            bytes.write(0, data, 0, data.length);
            assertEquals(1,
                    bytes.readByte(0),
                    "First byte should be written to the mapped region");
            assertEquals(4,
                    bytes.readByte(3),
                    "Last byte should be written to the mapped region");
        } finally {
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("write rejects data that exceeds write limit")
    public void writeRejectsWriteLimitOverflow() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes write limit test");
        File file = newTempFile();
        byte[] data = new byte[8];
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            bytes.writeLimit(8);
            assertThrows(BufferOverflowException.class,
                    () -> bytes.write(4, data, 0, 8),
                    "Write should fail when the write limit would be exceeded");
        } finally {
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("compareAndSwapLong rejects offsets beyond mapped capacity")
    public void compareAndSwapLongRejectsInvalidOffset() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes compareAndSwap bounds test");
        File file = newTempFile();
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            SingleMappedBytes mapped = (SingleMappedBytes) bytes;
            assertThrows(BufferOverflowException.class,
                    () -> mapped.compareAndSwapLong(mapped.capacity() + 1, 0L, 1L),
                    "compareAndSwapLong should reject offsets beyond capacity");
        } finally {
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("write from RandomDataInput copies bytes into the mapping")
    public void writeFromInputCopiesBytes() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes input copy test");
        File file = newTempFile();
        Bytes<?> input = Bytes.from("data");
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            bytes.write(0, input, 0, 4);
            assertEquals('d',
                    bytes.readByte(0),
                    "Data from RandomDataInput should be written into the mapping");
        } finally {
            input.releaseLast();
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("write from RandomDataInput rejects write limit overflow")
    public void writeFromInputRejectsOverflow() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes input limit test");
        File file = newTempFile();
        Bytes<?> input = Bytes.from("payload");
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            bytes.writeLimit(4);
            assertThrows(BufferOverflowException.class,
                    () -> bytes.write(0, input, 0, 8),
                    "Write should fail when the input length exceeds the write limit");
        } finally {
            input.releaseLast();
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("peekVolatileInt reads using aligned and unaligned paths")
    public void peekVolatileIntReadsValues() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes peekVolatileInt test");
        File file = newTempFile();
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 256)) {
            SingleMappedBytes mapped = (SingleMappedBytes) bytes;
            MappedBytesStore store = (MappedBytesStore) mapped.bytesStore();
            long base = store.address + store.translate(0);
            long offsetAligned = (64 - (base & 63)) & 63;
            long offsetUnaligned = offsetAligned + 61;
            mapped.writeInt(offsetAligned, 11);
            mapped.writeInt(offsetUnaligned, 22);
            mapped.writePosition(offsetUnaligned + 4);

            mapped.readPosition(offsetAligned);
            assertEquals(11,
                    mapped.peekVolatileInt(),
                    "Aligned position should read the stored value");
            mapped.readPosition(offsetUnaligned);
            assertEquals(22,
                    mapped.peekVolatileInt(),
                    "Unaligned position should read the stored value");
        } finally {
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("readPositionRemaining expands the write limit to requested size")
    public void readPositionRemainingExpandsWriteLimit() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes readPositionRemaining test");
        File file = newTempFile();
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            bytes.writeLimit(16);
            bytes.readPositionRemaining(0, 32);
            assertEquals(32,
                    bytes.writeLimit(),
                    "writeLimit should expand to match the requested remaining");
        } finally {
            deleteTempFile(file);
        }
    }

    @Test
    @DisplayName("compareAndSwapLong updates stored value when expected matches")
    public void compareAndSwapLongUpdatesValue() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory is required for mapped bytes compareAndSwap update test");
        File file = newTempFile();
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64)) {
            SingleMappedBytes mapped = (SingleMappedBytes) bytes;
            mapped.writeLong(0, 1L);
            assertEquals(1L,
                    mapped.readLong(0),
                    "Precondition value should be written before compareAndSwapLong");
            assertEquals(true,
                    mapped.compareAndSwapLong(0, 1L, 2L),
                    "compareAndSwapLong should succeed when expected matches");
            assertEquals(2L,
                    mapped.readLong(0),
                    "compareAndSwapLong should update the stored value");
        } finally {
            deleteTempFile(file);
        }
    }

    private static File newTempFile() {
        Path base = Paths.get(OS.getTarget());
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IORuntimeException("Unable to create target directory: " + base, e);
        }
        return base.resolve("singleMappedBytes-" + System.nanoTime() + ".dat").toFile();
    }

    private static void deleteTempFile(File file) {
        Path path = file.toPath();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IORuntimeException("Unable to delete temp file: " + path, e);
        }
    }
}
