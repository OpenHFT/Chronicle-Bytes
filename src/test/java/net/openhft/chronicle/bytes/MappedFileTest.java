/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.*;

public class MappedFileTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    private ThreadDump threadDump;

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Test
    public void testWarmup() throws InterruptedException {
        MappedFile.warmup();
    }

    @Test
    public void shouldReleaseReferenceWhenNewStoreIsAcquired() throws IOException {
        final File file = tmpDir.newFile();
        // this is what it will end up as
        final long chunkSize = OS.mapAlign(64);
        final MappedFile mappedFile = MappedFile.mappedFile(file, 64);
        final MappedBytesStore first = mappedFile.acquireByteStore(1);

        assertEquals(1L, first.refCount());

        final MappedBytesStore second = mappedFile.acquireByteStore(1 + chunkSize);

        assertEquals(1L, first.refCount());
        assertEquals(1L, second.refCount());

        final MappedBytesStore third = mappedFile.acquireByteStore(1 + chunkSize + chunkSize);

        assertEquals(1L, first.refCount());
        assertEquals(1L, second.refCount());
        assertEquals(1L, third.refCount());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testReferenceCounts() throws IOException {
        new File(OS.TARGET).mkdir();
        @NotNull File tmp = new File(OS.TARGET, "testReferenceCounts-" + System.nanoTime() + ".bin");
        tmp.deleteOnExit();
        int chunkSize = OS.isWindows() ? 64 << 10 : 4 << 10;
        @NotNull MappedFile mf = MappedFile.mappedFile(tmp, chunkSize, 0);
        assertEquals("refCount: 1", mf.referenceCounts());

        @Nullable MappedBytesStore bs = mf.acquireByteStore(chunkSize + (1 << 10));
        assertEquals(chunkSize, bs.start());
        assertEquals(chunkSize * 2, bs.capacity());
        Bytes bytes = bs.bytesForRead();

        assertNotNull(bytes.toString()); // show it doesn't blow up.
        assertNotNull(bs.toString()); // show it doesn't blow up.
        assertEquals(chunkSize, bytes.start());
        assertEquals(0L, bs.readLong(chunkSize + (1 << 10)));
        assertEquals(0L, bytes.readLong(chunkSize + (1 << 10)));
        Assert.assertFalse(bs.inside(chunkSize - (1 << 10)));
        Assert.assertFalse(bs.inside(chunkSize - 1));
        Assert.assertTrue(bs.inside(chunkSize));
        Assert.assertTrue(bs.inside(chunkSize * 2 - 1));
        Assert.assertFalse(bs.inside(chunkSize * 2));
        try {
            bytes.readLong(chunkSize - (1 << 10));
            Assert.fail();
        } catch (BufferUnderflowException e) {
            // expected
        }
        try {
            bytes.readLong(chunkSize * 2 + (1 << 10));
            Assert.fail();
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals(1, mf.refCount());
        assertEquals(2, bs.refCount());
        assertEquals("refCount: 1, 0, 2", mf.referenceCounts());

        @Nullable BytesStore bs2 = mf.acquireByteStore(chunkSize + (1 << 10));
        assertEquals(3, bs2.refCount());
        assertEquals("refCount: 1, 0, 3", mf.referenceCounts());
        bytes.release();
        assertEquals(2, bs2.refCount());
        assertEquals("refCount: 1, 0, 2", mf.referenceCounts());

        mf.release();
        assertEquals(0, bs.refCount());
        assertEquals(0, mf.refCount());
        assertEquals("refCount: 0, 0, 0", mf.referenceCounts());
    }

    @Test
    public void largeReadOnlyFile() throws IOException {
        if (Runtime.getRuntime().totalMemory() < Integer.MAX_VALUE || OS.isWindows())
            return;

        @NotNull File file = File.createTempFile("largeReadOnlyFile", "deleteme");
        file.deleteOnExit();
        try (@NotNull MappedBytes bytes = MappedBytes.mappedBytes(file, 1 << 30, OS.pageSize())) {
            bytes.writeLong(3L << 30, 0x12345678); // make the file 3 GB.
        }

        try (@NotNull MappedBytes bytes = MappedBytes.readOnly(file)) {
            Assert.assertEquals(0x12345678L, bytes.readLong(3L << 30));
        }
    }

    @Test
    public void interrupted() throws FileNotFoundException {
        Thread.currentThread().interrupt();
        String filename = OS.TARGET + "/interrupted-" + System.nanoTime();
        new File(filename).deleteOnExit();
        MappedFile mf = MappedFile.mappedFile(filename, 64 << 10, 0);
        try {
            mf.actualSize();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            mf.release();
        }
    }

    @Test
    public void testCreateMappedFile() throws IOException {
        File file = File.createTempFile("mappedFile", "");
        file.deleteOnExit();

        MappedFile mappedFile = MappedFile.mappedFile(file, 1024, 256, 256, false);
        MappedFile mappedFile2 = MappedFile.mappedFile(file, 1024, 256, 256, false);

        mappedFile.release();
        mappedFile2.release();
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }
}