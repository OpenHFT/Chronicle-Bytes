/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MappedFileTest {

    @Test
    public void testReferenceCounts() throws IOException {
        File tmp = new File(OS.TARGET, "testReferenceCounts-" + System.nanoTime() + ".bin");
        tmp.deleteOnExit();
        int chunkSize = OS.isWindows() ? 64 << 10 : 4 << 10;
        MappedFile mf = MappedFile.mappedFile(tmp, chunkSize, 0);
        assertEquals("refCount: 1", mf.referenceCounts());

        MappedBytesStore bs = mf.acquireByteStore(chunkSize + (1 << 10));
        assertEquals(chunkSize, bs.start());
        assertEquals(chunkSize * 2, bs.capacity());
        Bytes bytes = bs.bytes();
        // read everything which is there.
        bytes.readLimit(bytes.capacity());

        assertNotNull(bytes.toString()); // show it doesn't blow up.
        assertEquals(chunkSize, bytes.start());
        assertEquals(0L, bs.readLong(chunkSize + (1 << 10)));
        assertEquals(0L, bytes.readLong(chunkSize + (1 << 10)));
        Assert.assertFalse(bs.inStore(chunkSize - (1 << 10)));
        Assert.assertFalse(bs.inStore(chunkSize - 1));
        Assert.assertTrue(bs.inStore(chunkSize));
        Assert.assertTrue(bs.inStore(chunkSize * 2 - 1));
        Assert.assertFalse(bs.inStore(chunkSize * 2));
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

        mf.close();
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
}