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
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.*;

public class MappedFileTest extends BytesTestCommon {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    private ThreadDump threadDump;

    @After
    public void checkRegisteredBytes() {
        AbstractReferenceCounted.assertReferencesReleased();
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
    public void testWarmup() {
        MappedFile.warmup();
    }

    @Test
    public void shouldReleaseReferenceWhenNewStoreIsAcquired() throws IOException {
        final File file = tmpDir.newFile();
        // this is what it will end up as
        final long chunkSize = OS.mapAlign(64);
        ReferenceOwner test = ReferenceOwner.temporary("test");
        try (final MappedFile mappedFile = MappedFile.mappedFile(file, 64)) {
            final MappedBytesStore first = mappedFile.acquireByteStore(test, 1);

            int expected = MappedFile.RETAIN ? 2 : 1;
            assertEquals(expected, first.refCount());

            final MappedBytesStore second = mappedFile.acquireByteStore(test, 1 + chunkSize);

            assertEquals(expected, first.refCount());
            assertEquals(expected, second.refCount());

            final MappedBytesStore third = mappedFile.acquireByteStore(test, 1 + chunkSize + chunkSize);

            assertEquals(expected, first.refCount());
            assertEquals(expected, second.refCount());
            assertEquals(expected, third.refCount());

            third.release(test);
            second.release(test);
            first.release(test);
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testReferenceCounts() throws IOException {
        @NotNull File tmp = IOTools.createTempFile("testReferenceCounts");
        int chunkSize = OS.isWindows() ? 64 << 10 : 4 << 10;
        try (MappedFile mf = MappedFile.mappedFile(tmp, chunkSize, 0)) {
            assertEquals("refCount: 1", mf.referenceCounts());

            ReferenceOwner test = ReferenceOwner.temporary("test");
            @Nullable MappedBytesStore bs = mf.acquireByteStore(test, chunkSize + (1 << 10));
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
            int expected = MappedFile.RETAIN ? 2 : 1;
            assertEquals(expected + 1, bs.refCount());
            assertEquals("refCount: 1, 0, " + (expected + 1), mf.referenceCounts());

            @Nullable BytesStore bs2 = mf.acquireByteStore(test, chunkSize + (1 << 10), bs);
            assertSame(bs, bs2);
            assertEquals(expected + 1, bs2.refCount());
            assertEquals("refCount: 1, 0, " + (expected + 1), mf.referenceCounts());
            bytes.releaseLast();
            assertEquals(expected, bs2.refCount());
            assertEquals("refCount: 1, 0, " + expected, mf.referenceCounts());
            bs.release(test);
        }
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
    public void testReadOnlyOpen() throws IOException {
        String text = "Some text to put in this file. yay!\n";

        @NotNull File file = File.createTempFile("readOnlyOpenFile", "deleteme");

        // write some stuff to a file so it exits using stock java APIs
        @NotNull OutputStreamWriter outWrite = new OutputStreamWriter(new FileOutputStream(file));
        outWrite.append(text);
        outWrite.flush();
        outWrite.close();

        byte[] tmp = new byte[1024*16];

        // Open and read the file w/ a MappedBytes to show it's readable
        try (@NotNull MappedBytes mapBuf = MappedBytes.readOnly( file )) {
            mapBuf.readLimit( file.length() );
            int readLen = mapBuf.read(tmp, 0, tmp.length);
            assertEquals(text, new String(tmp, 0, readLen));
        }

        // open up the same file via a mapped file
        try (@NotNull MappedFile mapFile = MappedFile.mappedFile(file, OS.pageSize() * 16, OS.pageSize(), true)) {
            // this throws a exception as of v2.20.9. it shouldn't
            @NotNull Bytes buf = mapFile.acquireBytesForRead(ReferenceOwner.temporary("TEMP"), 0 );
            buf.readLimit( file.length() );
            int readLen = buf.read(tmp, 0, tmp.length);
            assertEquals(text, new String(tmp, 0, readLen));
        }



        file.deleteOnExit();
    }

    @Test
    public void interrupted() throws FileNotFoundException {
        Thread.currentThread().interrupt();
        String filename = IOTools.createTempFile("interrupted").getAbsolutePath();
        try (MappedFile mf = MappedFile.mappedFile(filename, 64 << 10, 0)) {
            mf.actualSize();
            assertTrue(Thread.currentThread().isInterrupted());
        }
    }

    @Test
    public void testCreateMappedFile() throws IOException {
        File file = IOTools.createTempFile("mappedFile");

        MappedFile mappedFile = MappedFile.mappedFile(file, 1024, 256, 256, false);
        MappedFile mappedFile2 = MappedFile.mappedFile(file, 1024, 256, 256, false);

        mappedFile.releaseLast();
        mappedFile2.releaseLast();
    }

    @After
    public void clearInterrupt() {
        Thread.interrupted();
    }
}