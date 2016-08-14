/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.junit.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MappedFileTest {

    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }
    @Ignore("todo fix sometimes fails on TC")
    @Test
    public void testReferenceCounts() throws IOException {
        new File(OS.TARGET).mkdir();
        File tmp = new File(OS.TARGET, "testReferenceCounts-" + System.nanoTime() + ".bin");
        tmp.deleteOnExit();
        int chunkSize = OS.isWindows() ? 64 << 10 : 4 << 10;
        MappedFile mf = MappedFile.mappedFile(tmp, chunkSize, 0);
        assertEquals("refCount: 1", mf.referenceCounts());

        MappedBytesStore bs = mf.acquireByteStore(chunkSize + (1 << 10));
        assertEquals(chunkSize, bs.start());
        assertEquals(chunkSize * 2, bs.capacity());
        Bytes bytes = bs.bytesForRead();

        assertNotNull(bytes.toString()); // show it doesn't blow up.
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
        assertEquals(2, mf.refCount());
        assertEquals(3, bs.refCount());
        assertEquals("refCount: 2, 0, 3", mf.referenceCounts());

        BytesStore bs2 = mf.acquireByteStore(chunkSize + (1 << 10));
        assertEquals(4, bs2.refCount());
        assertEquals("refCount: 2, 0, 4", mf.referenceCounts());
        bytes.release();
        assertEquals(3, bs2.refCount());
        assertEquals("refCount: 2, 0, 3", mf.referenceCounts());

        mf.release();
        assertEquals(2, bs.refCount());
        assertEquals("refCount: 1, 0, 2", mf.referenceCounts());
        bs2.release();
        assertEquals(1, mf.refCount());
        assertEquals(1, bs.refCount());
        bs.release();
        assertEquals(0, bs.refCount());
        assertEquals(0, mf.refCount());
        assertEquals("refCount: 0, 0, 0", mf.referenceCounts());
    }

    @Test
    public void largeReadOnlyFile() throws IOException {
        File file = File.createTempFile("largeReadOnlyFile", "deleteme");
        file.deleteOnExit();
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, 1 << 30, OS.pageSize())) {
            bytes.writeLong(3L << 30, 0x12345678); // make the file 3 GB.
        }

        try (MappedBytes bytes = MappedBytes.readOnly(file)) {
            Assert.assertEquals(0x12345678L, bytes.readLong(3L << 30));
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void readOnlyCantCreateNonExistentFile() throws IOException {
        MappedBytes.mappedBytes(new File("non_existent_file"), OS.pageSize(), OS.pageSize(), true);
    }
}