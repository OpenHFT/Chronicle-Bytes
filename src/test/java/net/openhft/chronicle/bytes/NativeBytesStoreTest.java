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
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Created by peter.lawrey on 27/02/15.
 */
public class NativeBytesStoreTest {
    @Test
    public void testElasticByteBuffer() throws IORuntimeException, BufferOverflowException {
        Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        assertEquals(Bytes.MAX_CAPACITY, bbb.capacity());
        assertEquals(OS.pageSize(), bbb.realCapacity());
        ByteBuffer bb = bbb.underlyingObject();
        assertNotNull(bb);

        for (int i = 0; i < 16; i++) {
            bbb.writeSkip(1000);
            bbb.writeLong(12345);
        }
        assertEquals(OS.pageSize() * 4, bbb.realCapacity());
        ByteBuffer bb2 = bbb.underlyingObject();
        assertNotNull(bb2);
        assertNotSame(bb, bb2);
    }
}
