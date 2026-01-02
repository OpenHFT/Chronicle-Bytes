/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Mapped file behaviours for reference counts and IO")
public class MappedFileTest extends BytesTestCommon {

    @TempDir
    Path tmpDir;

    @SuppressWarnings("java:S5826") // JUnit 4 lifecycle retained; class mixes Vintage and Jupiter
    @BeforeEach
    public void ignoreCouldntDisable() {
        if (Jvm.maxDirectMemory() == 0) {
            ignoreException("Couldn't disable close on interrupt");
            ignoreException("class is not public");
        }
    }

    @Test
    @DisplayName("mapped file warmup runs with direct memory")
    void testWarmup() {
        try {
            if (Jvm.maxDirectMemory() > 0)
                MappedFile.warmup();
        } catch (Throwable t) {
            fail("Mapped file warmup failed " + t.getMessage());
        }
    }

    @Test
    @DisplayName("reference counts reset when new store acquired")
    public void shouldReleaseReferenceWhenNewStoreIsAcquired()
            throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped file ref count test");
        final File file = Files.createTempFile(tmpDir, "mapped-file", ".tmp").toFile();
        // this is what it will end up as
        final long chunkSize = OS.mapAlign(64);
        final ReferenceOwner test = ReferenceOwner.temporary("test");
        try (final MappedFile mappedFile = MappedFile.mappedFile(file, 64, 0)) {
            final MappedBytesStore first = mappedFile.acquireByteStore(test, 1);

            final int expected = MappedFile.RETAIN ? 2 : 1;
            assertEquals(expected, first.refCount(),
                    "First store ref count matches expected after acquire");

            final MappedBytesStore second = mappedFile.acquireByteStore(test, 1 + chunkSize);

            assertEquals(expected, first.refCount(),
                    "First store ref count remains expected after second acquire");
            assertEquals(expected, second.refCount(),
                    "Second store ref count matches expected after acquire");

            final MappedBytesStore third = mappedFile.acquireByteStore(test, 1 + chunkSize + chunkSize);

            assertEquals(expected, first.refCount(),
                    "First store ref count remains expected after third acquire");
            assertEquals(expected, second.refCount(),
                    "Second store ref count remains expected after third acquire");
            assertEquals(expected, third.refCount(),
                    "Third store ref count matches expected after acquire");

            third.release(test);
            second.release(test);
            first.release(test);
        }
    }

    @Test
    @DisplayName("reference counts reflect store acquisitions and releases")
    public void testReferenceCounts()
            throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for reference count test");

        final File tmp = IOTools.createTempFile("testReferenceCounts");
        int chunkSize;
        if (OS.isWindows()) {
            chunkSize = 64;
        } else if (Jvm.isMacArm()) {
            chunkSize = 16;
        } else
            chunkSize = 4;
        chunkSize = chunkSize / 4 * PageUtil.getPageSize(tmp.getAbsolutePath());

        try (MappedFile mf = MappedFile.mappedFile(tmp, chunkSize, 0)) {
            assertEquals("refCount: 1", mf.referenceCounts(),
                    "Initial mapped file reference counts string matches");

            final ReferenceOwner test = ReferenceOwner.temporary("test");
            final MappedBytesStore bs = mf.acquireByteStore(test, chunkSize + (1 << 10));

            try {
                assertEquals(chunkSize, bs.start(),
                        "Byte store start equals chunk size");
                assertEquals(chunkSize * 2, bs.capacity(),
                        "Byte store capacity doubles chunk size");
                final Bytes<?> bytes = bs.bytesForRead();

                assertNotNull(bytes.toString(),
                        "Bytes toString produces non null output"); // show it doesn't blow up.
                assertNotNull(bs.toString(),
                        "BytesStore toString produces non null output"); // show it doesn't blow up.
                assertEquals(chunkSize, bytes.start(),
                        "Bytes start equals chunk size");
                assertEquals(0L, bs.readLong(chunkSize + (1 << 10)),
                        "BytesStore reads zero at extended offset");
                assertEquals(0L, bytes.readLong(chunkSize + (1 << 10)),
                        "Bytes reads zero at extended offset");
                assertFalse(bs.inside(chunkSize - (1 << 10)),
                        "inside returns false below start");
                assertFalse(bs.inside(chunkSize - 1),
                        "inside returns false below boundary");
                assertTrue(bs.inside(chunkSize),
                        "inside returns true at start boundary");
                assertTrue(bs.inside(chunkSize * 2L - 1),
                        "inside returns true at end boundary");
                assertFalse(bs.inside(chunkSize * 2L),
                        "inside returns false beyond end");
                int finalChunkSize = chunkSize;
                assertThrows(BufferUnderflowException.class,
                        () -> bytes.readLong(finalChunkSize - (1 << 10)),
                        "Read before start should underflow");
                assertThrows(BufferUnderflowException.class,
                        () -> bytes.readLong(finalChunkSize * 2L + (1 << 10)),
                        "Read beyond end should underflow");
                assertEquals(1, mf.refCount(),
                        "Mapped file ref count returns to one");
                final int expected = MappedFile.RETAIN ? 2 : 1;
                assertEquals(expected + 1, bs.refCount(),
                        "BytesStore ref count includes extra retain");
                assertEquals("refCount: 1, 0, " + (expected + 1), mf.referenceCounts(),
                        "Reference counts string includes bytes store retain");

                final BytesStore<?, ?> bs2 = mf.acquireByteStore(test, chunkSize + (1 << 10), bs);
                assertSame(bs, bs2,
                        "Acquire with existing store returns same instance");
                assertEquals(expected + 1, bs2.refCount(),
                        "Reacquired store ref count remains expected");
                assertEquals("refCount: 1, 0, " + (expected + 1), mf.referenceCounts(),
                        "Reference counts string unchanged after re-acquire");
                bytes.releaseLast();
                assertEquals(expected, bs2.refCount(),
                        "Ref count drops after bytes release");
                assertEquals("refCount: 1, 0, " + expected, mf.referenceCounts(),
                        "Reference counts string reflects released store");
            } finally {
                bs.release(test);
            }
        }
    }

    @Test
    @DisplayName("large read only file can be mapped")
    public void largeReadOnlyFile() throws IOException {
        assumeFalse(Runtime.getRuntime().maxMemory() < Integer.MAX_VALUE || OS.isWindows(),
                "Large read only test requires non Windows with enough heap");
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for large read only test");

        final File file = Files.createTempFile("largeReadOnlyFile", "deleteme").toFile();
        file.deleteOnExit();
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, 1 << 30, OS.pageSize())) {
            bytes.writeLong(3L << 30, 0x12345678); // make the file 3 GB.
        }

        try (MappedBytes bytes = MappedBytes.readOnly(file)) {
            assertEquals(0x12345678L, bytes.readLong(3L << 30),
                    "Read only mapping returns expected long value");
        }
    }

    @Test
    @DisplayName("single mapped large file can be read")
    public void largeReadOnlyFileSingle() throws IOException {
        assumeFalse(OS.isWindows(),
                "Single mapped large file test requires non Windows");
        assumeFalse(Runtime.getRuntime().maxMemory() < Integer.MAX_VALUE,
                "Single mapped large file test requires enough heap");
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for single mapped file test");

        final File file = Files.createTempFile("largeReadOnlyFile", "deleteme").toFile();
        file.deleteOnExit();
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 4L << 30)) {
            bytes.writeLong(3L << 30, 0x12345678); // make the file 3 GB.
        }

        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 4L << 30, false)) {
            assertEquals(0x12345678L, bytes.readLong(3L << 30),
                    "Single mapped file returns expected long value");
        }
    }

    @Test
    @DisplayName("mapped file preserves interrupt status flag")
    public void interrupted() throws Exception {
        ignoreException("/proc/self/mountinfo");
        Thread.currentThread().interrupt();
        final String filename = IOTools.createTempFile("interrupted").getAbsolutePath();
        try (MappedFile mf = MappedFile.mappedFile(filename, 64 << 10, 0)) {
            mf.actualSize();
            assertTrue(Thread.currentThread().isInterrupted(),
                    "Interrupt flag remains set after mapped file access");
        }
    }

    @Test
    @DisplayName("mapped file close state reflects releases")
    public void testCreateMappedFile() throws Exception {
        final File file = IOTools.createTempFile("mappedFile");

        final MappedFile mappedFile = MappedFile.mappedFile(file, 1024, 256, 256, false);
        try {
            final MappedFile mappedFile2 = MappedFile.mappedFile(file, 1024, 256, 256, false);
            mappedFile2.releaseLast();
            assertTrue(mappedFile2.isClosed(),
                    "Secondary mapped file reports closed after release");
        } finally {
            mappedFile.releaseLast();
        }
        assertTrue(mappedFile.isClosed(),
                "Primary mapped file reports closed after release");
    }

    @Test
    @DisplayName("read only mapped file reads expected text")
    public void testReadOnlyOpen()
            throws IOException {
        assumeFalse(OS.isWindows(),
                "Read only mapped file test requires non Windows");
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for read only file test");

        String text = "Some text to put in this file. yay!\n";

        @NotNull File file = Files.createTempFile("readOnlyOpenFile", "deleteme").toFile();

        // write some stuff to a file so it exits using stock java APIs
        try (OutputStreamWriter outWrite = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.ISO_8859_1)) {
            outWrite.append(text);
        }

        byte[] tmp = new byte[1024 * 16];

        // Open and read the file w/ a MappedBytes to show it's readable
        try (@NotNull MappedBytes mapBuf = MappedBytes.readOnly(file)) {
            mapBuf.readLimit(file.length());
            int readLen = mapBuf.read(tmp, 0, tmp.length);
            assertEquals(text, new String(tmp, 0, readLen, StandardCharsets.ISO_8859_1),
                    "Read only bytes return expected file text");
        }

        // open up the same file via a mapped file
        try (@NotNull MappedFile mapFile = MappedFile.mappedFile(file, OS.pageSize() * 16L, OS.pageSize(), true)) {
            // this throws a exception as of v2.20.9. it shouldn't
            ReferenceOwner temp = ReferenceOwner.temporary("TEMP");
            @NotNull Bytes<?> buf = mapFile.acquireBytesForRead(temp, 0);
            buf.readLimit(file.length());
            int readLen = buf.read(tmp, 0, tmp.length);
            assertEquals(text, new String(tmp, 0, readLen, StandardCharsets.ISO_8859_1),
                    "Mapped file bytes return expected file text");
            buf.releaseLast(temp);
        }

        file.deleteOnExit();
    }

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }
}
