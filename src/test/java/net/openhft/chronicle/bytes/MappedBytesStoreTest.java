/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class MappedBytesStoreTest extends BytesTestCommon implements ReferenceOwner {
    private MappedFile mappedFile;
    private MappedBytesStore mappedBytesStore;

    @Before
    public void setup() throws IOException {
        String filePath = OS.getTarget() + "/test" + System.nanoTime() + ".deleteme";
        mappedFile = MappedFile.mappedFile(filePath, OS.pageSize(), OS.pageSize());
        mappedBytesStore = mappedFile.acquireByteStore(this, 0);
        new File(filePath).deleteOnExit();
    }

    @After
    public void tearDown() {
        if (mappedBytesStore != null)
            mappedBytesStore.release(this);
        mappedFile.close();
    }

    @Test
    public void testWriteReadBytes() throws ClosedIllegalStateException {
        byte value = 123;
        long position = 5;
        mappedBytesStore.writeByte(position, value);

        byte readValue = mappedBytesStore.readByte(position);
        assertEquals("Written and read values should be equal", value, readValue);
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteAfterClose() {
        try {
            mappedBytesStore.release(this);
            mappedBytesStore.release(ReferenceOwner.INIT);
            mappedBytesStore.writeByte(0, (byte) 1);
        } finally {
            mappedBytesStore = null;
        }
    }

    @Test
    public void testSafeLimit() {
        assertTrue("Position within safe limit should be valid", mappedBytesStore.inside(0));
        assertFalse("Position beyond safe limit should be invalid", mappedBytesStore.inside(mappedBytesStore.safeLimit()));
    }

    @Test
    public void testCapacity() {
        assertEquals("The capacities should match", OS.pageSize() * 2, mappedBytesStore.capacity());
    }

    @Test
    public void testLockRegion() throws IOException {
        // Try to lock a region of the file
        assertNotNull("Lock should be obtained", mappedBytesStore.tryLock(0, 10, true));
    }

    @Test
    public void testByteBufferReadWrite() throws ClosedIllegalStateException {
        byte[] writeBytes = new byte[10];
        for (byte i = 0; i < 10; i++) {
            writeBytes[i] = i;
        }
        mappedBytesStore.write(0, writeBytes, 0, writeBytes.length);

        byte[] readBytes = new byte[10];
        mappedBytesStore.read(0, readBytes, 0, 10);

        assertArrayEquals("Buffer content should match", writeBytes, readBytes);
    }

    @Test
    public void testSyncUpTo() throws IOException {
        mappedBytesStore.syncUpTo(0);
        mappedBytesStore.syncUpTo(1000);
        mappedBytesStore.syncUpTo(5000);
        mappedBytesStore.syncUpTo(1000000);

        mappedBytesStore.release(this);
        mappedBytesStore = mappedFile.acquireByteStore(this, OS.pageSize());

        mappedBytesStore.syncUpTo(0);
        mappedBytesStore.syncUpTo(1000);
        mappedBytesStore.syncUpTo(5000);
        mappedBytesStore.syncUpTo(1000000);
    }
}
