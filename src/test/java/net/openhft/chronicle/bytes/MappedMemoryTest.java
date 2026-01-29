/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static net.openhft.chronicle.bytes.MappedBytes.mappedBytes;
import static net.openhft.chronicle.bytes.MappedBytes.singleMappedBytes;
import static net.openhft.chronicle.bytes.MappedFile.mappedFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests mapped memory performance and reference counting because these
 * metrics are critical to ensure low-latency access and proper resource
 * cleanup in high-frequency trading systems. In order to prevent memory
 * leaks, reference counts must be decremented correctly after use.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("Mapped memory performance and reference count tracking validation")
public class MappedMemoryTest extends BytesTestCommon {

    private static final long SHIFT = 27L;
    private static final long BLOCK_SIZE = 1L << SHIFT;

    @BeforeEach
    public void directEnabled() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped memory tests");
    }

    // on i7-3970X ~ 3.3 ns
    @Test
    @DisplayName("raw memory mapped writes update ref counts")
    public void testRawMemoryMapped()
            throws IOException {

        final ReferenceOwner test = ReferenceOwner.temporary("test");
        for (int t = 0; t < 5; t++) {
            final File tempFile = Files.createTempFile("chronicle", "q").toFile();
            try {

                final long startTime = System.nanoTime();
                MappedFile file0;
                try (MappedFile mappedFile = mappedFile(tempFile, BLOCK_SIZE / 2, OS.pageSize())) {
                    file0 = mappedFile;
                    final MappedBytesStore bytesStore = mappedFile.acquireByteStore(test, 1);
                    final long address = bytesStore.address;

                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        OS.memory().writeLong(address + i, i);
                    }
                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        OS.memory().writeLong(address + i, i);
                    }
                    bytesStore.release(test);
                }
                assertEquals(0, file0.refCount(),
                        "Mapped file ref count resets after store release on run " + t + " " + file0.referenceCounts());
                double nanosPerLong = 8.0 * (System.nanoTime() - startTime) / BLOCK_SIZE;
                Jvm.perf().on(getClass(), "With RawMemory,\t\t time= " + nanosPerLong
                        + " ns, number of longs written=" + (BLOCK_SIZE / 8));
            } finally {
                deleteIfPossible(tempFile);
            }
        }
    }

    // on i7-3970X ~ 6.9 ns
    @Test
    @DisplayName("mapped native bytes writes keep ref counts")
    public void withMappedNativeBytesTest()
            throws IOException {

        for (int t = 0; t < 3; t++) {
            final File tempFile = Files.createTempFile("chronicle", "q").toFile();
            try {

                final long startTime = System.nanoTime();
                final Bytes<?> bytes = mappedBytes(tempFile, BLOCK_SIZE / 2);
                for (long i = 0; i < BLOCK_SIZE; i += 8) {
                    bytes.writeLong(i);
                }
                bytes.releaseLast();
                assertEquals(0, bytes.refCount(),
                        "Mapped bytes ref count resets after release on run " + t);
                double nanosPerLong = 8.0 * (System.nanoTime() - startTime) / BLOCK_SIZE;
                Jvm.perf().on(getClass(), "With MappedNativeBytes, avg time= " + nanosPerLong
                        + " ns, number of longs written=" + (BLOCK_SIZE / 8));
            } finally {
                deleteIfPossible(tempFile);
            }
        }
    }

    // on i7-3970X ~ 6.0 ns
    @Test
    @DisplayName("raw native bytes writes run without errors")
    public void withRawNativeBytesTess()
            throws IOException {
        final ReferenceOwner test = ReferenceOwner.temporary("test");

        for (int t = 0; t < 3; t++) {
            final File tempFile = Files.createTempFile("chronicle", "q").toFile();
            try {

                final long startTime = System.nanoTime();
                try (MappedFile mappedFile = mappedFile(tempFile, BLOCK_SIZE / 2, OS.pageSize())) {
                    Bytes<?> bytes = mappedFile.acquireBytesForWrite(test, 1);
                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        bytes.writeLong(i);
                    }
                    bytes.releaseLast(test);

                    bytes = mappedFile.acquireBytesForWrite(test, BLOCK_SIZE / 2 + 1);
                    for (long i = 0; i < BLOCK_SIZE / 2; i += 8L) {
                        bytes.writeLong(i);
                    }
                    bytes.releaseLast(test);

                    double nanosPerLong = 8.0 * (System.nanoTime() - startTime) / BLOCK_SIZE;
                    Jvm.perf().on(getClass(), "With NativeBytes,\t\t time= " + nanosPerLong
                            + " ns, number of longs written=" + (BLOCK_SIZE / 8));
                } catch (Throwable throwable) {
                    // Performance test so just make sure the test ran
                    fail("Mapped native bytes perf test threw exception on run " + t + " " + throwable.getMessage());
                }
            } finally {
                deleteIfPossible(tempFile);
            }
        }
    }

    @Test
    @DisplayName("mapped bytes reserve and release update ref counts")
    public void mappedMemoryTest()
            throws IOException, IORuntimeException {

        final File tempFile = Files.createTempFile("chronicle", "q").toFile();
        Bytes<?> bytes0;
        try {
            try (MappedBytes bytes = mappedBytes(tempFile, OS.pageSize())) {
                bytes0 = bytes;
                final ReferenceOwner test = ReferenceOwner.temporary("test");
                try {
                    assertEquals(1, bytes.refCount(),
                            "Mapped bytes start with ref count one");
                    bytes.reserve(test);
                    assertEquals(2, bytes.refCount(),
                            "Mapped bytes ref count increases after reserve");

                    // The page size is 0x4000 on Mac M1 (and not 0x1000) so we need to stay in reasonable bounds
                    final char[] chars = new char[OS.pageSize() * 7];

                    Arrays.fill(chars, '.');
                    chars[chars.length - 1] = '*';
                    bytes.writeUtf8(new String(chars));

                    final int pos = Math.toIntExact(bytes.writePosition());

                    final String text = "hello this is some very long text";
                    bytes.writeUtf8(text);
                    final String textValue = bytes.toString();
                    assertEquals(text, textValue.substring(pos + 1),
                            "Mapped bytes text contains appended content");
                    assertEquals(2, bytes.refCount(),
                            "Mapped bytes ref count remains two after write");
                } finally {
                    bytes.release(test);
                    assertEquals(1, bytes.refCount(),
                            "Mapped bytes ref count returns after release");
                }
            }
        } finally {
            deleteIfPossible(tempFile);
        }
        assertEquals(0, bytes0.refCount(),
                "Mapped bytes ref count is zero after close");
    }

    @Test
    @DisplayName("single mapped bytes reserve and release update ref counts")
    public void mappedMemoryTestSingle()
            throws IOException, IORuntimeException {

        final File tempFile = Files.createTempFile("chronicle", "q").toFile();
        Bytes<?> bytes0;
        try {
            try (MappedBytes bytes = singleMappedBytes(tempFile, OS.pageSize() * 8L)) {
                bytes0 = bytes;
                final ReferenceOwner test = ReferenceOwner.temporary("test");
                try {
                    assertEquals(1, bytes.refCount(),
                            "Single mapped bytes start with ref count one");
                    bytes.reserve(test);
                    assertEquals(2, bytes.refCount(),
                            "Single mapped bytes ref count increases after reserve");

                    // The page size is 0x4000 on Mac M1 (and not 0x1000) so we need to stay in reasonable bounds
                    final char[] chars = new char[OS.pageSize() * 7];

                    Arrays.fill(chars, '.');
                    chars[chars.length - 1] = '*';
                    bytes.writeUtf8(new String(chars));

                    final int pos = Math.toIntExact(bytes.writePosition());

                    final String text = "hello this is some very long text";
                    bytes.writeUtf8(text);
                    final String textValue = bytes.toString();
                    assertEquals(text, textValue.substring(pos + 1),
                            "Single mapped bytes text contains appended content");
                    assertEquals(2, bytes.refCount(),
                            "Single mapped bytes ref count remains two after write");
                } finally {
                    bytes.release(test);
                    assertEquals(1, bytes.refCount(),
                            "Single mapped bytes ref count returns after release");
                }
            }
        } finally {
            if (OS.isWindows())
                ignoreException("Unable to delete");
            deleteIfPossible(tempFile);
        }
        assertEquals(0, bytes0.refCount(),
                "Single mapped bytes ref count is zero after close");
    }
}
