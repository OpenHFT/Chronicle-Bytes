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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
class MappedFileTest extends BytesTestCommon {

    @TempDir
    public File tmpDir;

    @SuppressWarnings("java:S5826") // JUnit 4 lifecycle retained; class mixes Vintage and Jupiter
    @BeforeEach
    public void ignoreCouldntDisable() {
        if (Jvm.maxDirectMemory() == 0) {
            ignoreException("Couldn't disable close on interrupt");
            ignoreException("class is not public");
        }
    }

    @org.junit.jupiter.api.Test
    public void insideHonoursSafeLimitWhenPageSizeDiffers() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        final File file = File.createTempFile("junit", null, tmpDir);
        final long chunkSize = OS.pageAlign(64 << 10);
        final long overlapSize = OS.pageAlign(4 << 10);
        final int enlargedPageSize = Math.max(OS.pageSize(), 4096) * 2;
        final ReferenceOwner owner = ReferenceOwner.temporary("page-matrix");

        try (MappedFile mappedFile = MappedFile.of(file, chunkSize, overlapSize, enlargedPageSize, false)) {
            final MappedBytesStore store = mappedFile.acquireByteStore(owner, chunkSize);
            try {
                final long safeLimit = store.safeLimit();
                assertEquals(store.start() + chunkSize, safeLimit, "safe limit should be positioned at start plus chunk size boundary");
                assertTrue(store.inside(safeLimit - 1), "position one byte before safe limit should be considered inside the mapped store");
                assertFalse(store.inside(safeLimit), "position at safe limit boundary should not be considered inside the mapped store");
            } finally {
                store.release(owner);
            }
        }
    }

    @org.junit.jupiter.api.Test
    void testWarmup() {
        try {
            if (Jvm.maxDirectMemory() > 0)
                MappedFile.warmup();
        } catch (Throwable t) {
            fail(t.getMessage());
        }
    }

    @org.junit.jupiter.api.Test
    public void shouldReleaseReferenceWhenNewStoreIsAcquired()
            throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        final File file = File.createTempFile("junit", null, tmpDir);
        // this is what it will end up as
        final long chunkSize = OS.mapAlign(64);
        final ReferenceOwner test = ReferenceOwner.temporary("test");
        try (final MappedFile mappedFile = MappedFile.mappedFile(file, 64, 0)) {
            final MappedBytesStore first = mappedFile.acquireByteStore(test, 1);

            final int expected = MappedFile.RETAIN ? 2 : 1;
            assertEquals(expected, first.refCount(), "first store reference count should match expected value based on retain policy");

            final MappedBytesStore second = mappedFile.acquireByteStore(test, 1 + chunkSize);

            assertEquals(expected, first.refCount(), "first store reference count should remain unchanged when acquiring second store");
            assertEquals(expected, second.refCount(), "second store reference count should match expected value based on retain policy");

            final MappedBytesStore third = mappedFile.acquireByteStore(test, 1 + chunkSize + chunkSize);

            assertEquals(expected, first.refCount(), "first store reference count should remain unchanged when acquiring third store");
            assertEquals(expected, second.refCount(), "second store reference count should remain unchanged when acquiring third store");
            assertEquals(expected, third.refCount(), "third store reference count should match expected value based on retain policy");

            third.release(test);
            second.release(test);
            first.release(test);
        }
    }

    @org.junit.jupiter.api.Test
    public void testReferenceCounts()
            throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final File tmp = IOTools.createTempFile("testReferenceCounts");
        int chunkSize;
        if (OS.isWindows()) {
            chunkSize = 64;
        } else if (Jvm.isMacArm()) {
            chunkSize = 16;
        } else {
            chunkSize = 4;
        }
        chunkSize = chunkSize / 4 * PageUtil.getPageSize(tmp.getAbsolutePath());

        final int cs = chunkSize;
        try (MappedFile mf = MappedFile.mappedFile(tmp, cs, 0)) {
            assertEquals("refCount: 1", mf.referenceCounts(), "newly created mapped file should have single reference count with no byte stores allocated");

            final ReferenceOwner test = ReferenceOwner.temporary("test");
            final MappedBytesStore bs = mf.acquireByteStore(test, cs + (1 << 10));

            try {
                assertEquals(cs, bs.start(), "byte store should start at chunk size boundary aligned to requested position");
                assertEquals(cs * 2L, bs.capacity(), "byte store capacity should span two complete chunks for chunk-aligned acquisition");
                final Bytes<?> bytes = bs.bytesForRead();

                assertNotNull(bytes.toString(), "bytes string representation should be safely convertible without throwing exceptions"); // show it doesn't blow up.
                assertNotNull(bs.toString(), "byte store string representation should be safely convertible without throwing exceptions"); // show it doesn't blow up.
                assertEquals(cs, bytes.start(), "bytes view should inherit start position from underlying byte store");
                assertEquals(0L, bs.readLong(cs + (1 << 10)), "reading uninitialized memory in mapped file should return zero value");
                assertEquals(0L, bytes.readLong(cs + (1 << 10)), "reading uninitialized memory through bytes view should return zero value");
                assertFalse(bs.inside(cs - (1 << 10)), "position well before start boundary should not be considered inside byte store");
                assertFalse(bs.inside(cs - 1), "position immediately before start boundary should not be considered inside byte store");
                assertTrue(bs.inside(cs), "position at start boundary should be considered inside byte store");
                assertTrue(bs.inside(cs * 2L - 1), "position one byte before capacity limit should be considered inside byte store");
                assertFalse(bs.inside(cs * 2L), "position at capacity boundary should not be considered inside byte store");
                assertThrows(BufferUnderflowException.class, () -> bytes.readLong(cs - (1 << 10)));
                assertThrows(BufferUnderflowException.class, () -> bytes.readLong(cs * 2L + (1 << 10)));
                assertEquals(1, mf.refCount(), "mapped file should maintain single reference count independent of byte store acquisitions");
                final int expected = MappedFile.RETAIN ? 2 : 1;
                assertEquals(expected + 1, bs.refCount(), "byte store reference count should include mapped file reference plus bytes view reference");
                assertEquals("refCount: 1, 0, " + (expected + 1), mf.referenceCounts(), "mapped file reference counts should show file reference and allocated byte store at second chunk position");

                final BytesStore<?, ?> bs2 = mf.acquireByteStore(test, chunkSize + (1 << 10), bs);
                assertSame(bs, bs2, "acquiring same BytesStore twice with hint should return identical instance");
                assertEquals(expected + 1, bs2.refCount(), "re-acquired byte store reference count should remain unchanged when returning same instance");
                assertEquals("refCount: 1, 0, " + (expected + 1), mf.referenceCounts(), "mapped file reference counts should remain unchanged when re-acquiring same byte store");
                bytes.releaseLast();
                assertEquals(expected, bs2.refCount(), "byte store reference count should decrement after releasing bytes view reference");
                assertEquals("refCount: 1, 0, " + expected, mf.referenceCounts(), "mapped file reference counts should reflect released bytes view");
            } finally {
                bs.release(test);
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void largeReadOnlyFile() throws IOException {
        assumeFalse(Runtime.getRuntime().maxMemory() < Integer.MAX_VALUE || OS.isWindows());
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final File file = Files.createTempFile("largeReadOnlyFile", "deleteme").toFile();
        file.deleteOnExit();
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, 1 << 30, OS.pageSize())) {
            bytes.writeLong(3L << 30, 0x12345678); // make the file 3 GB.
        }

        try (MappedBytes bytes = MappedBytes.readOnly(file)) {
            assertEquals(0x12345678L, bytes.readLong(3L << 30), "read-only mapped bytes should correctly retrieve value written at 3gb offset in large file");
        }
    }

    @org.junit.jupiter.api.Test
    public void largeReadOnlyFileSingle() throws IOException {
        assumeFalse(OS.isWindows());
        assumeFalse(Runtime.getRuntime().maxMemory() < Integer.MAX_VALUE);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final File file = Files.createTempFile("largeReadOnlyFile", "deleteme").toFile();
        file.deleteOnExit();
        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 4L << 30)) {
            bytes.writeLong(3L << 30, 0x12345678); // make the file 3 GB.
        }

        try (MappedBytes bytes = MappedBytes.singleMappedBytes(file, 4L << 30, false)) {
            assertEquals(0x12345678L, bytes.readLong(3L << 30), "single mapped bytes should correctly retrieve value written at 3gb offset in large file");
        }
    }

    @org.junit.jupiter.api.Test
    public void interrupted() throws Exception {
        ignoreException("/proc/self/mountinfo");
        Thread.currentThread().interrupt();
        final String filename = IOTools.createTempFile("interrupted").getAbsolutePath();
        try (MappedFile mf = MappedFile.mappedFile(filename, 64 << 10, 0)) {
            mf.actualSize();
            assertTrue(Thread.currentThread().isInterrupted(), "thread interrupt flag should be preserved across mapped file operations");
        }
    }

    @org.junit.jupiter.api.Test
    public void testCreateMappedFile() throws Exception {
        final File file = IOTools.createTempFile("mappedFile");

        final MappedFile mappedFile = MappedFile.mappedFile(file, 1024, 256, 256, false);
        try {
            final MappedFile mappedFile2 = MappedFile.mappedFile(file, 1024, 256, 256, false);
            mappedFile2.releaseLast();
            assertTrue(mappedFile2.isClosed(), "second mapped file instance should be closed after releasing when sharing same underlying file");
        } finally {
            mappedFile.releaseLast();
        }
        assertTrue(mappedFile.isClosed(), "first mapped file instance should be closed after releasing final reference");
    }

    @org.junit.jupiter.api.Test
    public void testReadOnlyOpen()
            throws IOException {
        assumeFalse(OS.isWindows());
        assumeFalse(Jvm.maxDirectMemory() == 0);

        String text = "Some text to put in this file. yay!\n";

        @NotNull File file = Files.createTempFile("readOnlyOpenFile", "deleteme").toFile();

        // write some stuff to a file so it exits using stock java APIs
        @NotNull OutputStreamWriter outWrite = new OutputStreamWriter(Files.newOutputStream(file.toPath()), ISO_8859_1);
        outWrite.append(text);
        outWrite.flush();
        outWrite.close();

        byte[] tmp = new byte[1024 * 16];

        // Open and read the file w/ a MappedBytes to show it's readable
        try (@NotNull MappedBytes mapBuf = MappedBytes.readOnly(file)) {
            mapBuf.readLimit(file.length());
            int readLen = mapBuf.read(tmp, 0, tmp.length);
            assertEquals(text, new String(tmp, 0, readLen, ISO_8859_1), "read-only mapped bytes should correctly read text content written to file");
        }

        // open up the same file via a mapped file
        try (@NotNull MappedFile mapFile = MappedFile.mappedFile(file, OS.pageSize() * 16L, OS.pageSize(), true)) {
            // this throws a exception as of v2.20.9. it shouldn't
            ReferenceOwner temp = ReferenceOwner.temporary("TEMP");
            @NotNull Bytes<?> buf = mapFile.acquireBytesForRead(temp, 0);
            buf.readLimit(file.length());
            int readLen = buf.read(tmp, 0, tmp.length);
            assertEquals(text, new String(tmp, 0, readLen, ISO_8859_1), "read-only mapped file should correctly read text content through acquired bytes view");
            buf.releaseLast(temp);
        }

        file.deleteOnExit();
    }

    @AfterEach
    public void clearInterrupt() {
        Thread.interrupted();
    }
}
