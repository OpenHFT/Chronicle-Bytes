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
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by peter.lawrey on 27/02/15.
 */
public class NativeBytesStoreTest {
    volatile int bcs;

    @Test
    public void testElasticByteBuffer() throws IORuntimeException, BufferOverflowException {
        Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        assertEquals(Bytes.MAX_CAPACITY, bbb.capacity());
        assertEquals(Bytes.DEFAULT_BYTE_BUFFER_CAPACITY, bbb.realCapacity());
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

    @Test
    public void testAppendUtf8() {
        String hi = "Hello World";
        char[] chars = hi.toCharArray();
        NativeBytesStore nbs = NativeBytesStore.nativeStore(chars.length);
        nbs.appendUtf8(0, chars, 0, chars.length);
        assertEquals(hi, nbs.toString());
    }

    @Test
    public void perfCheckSum() {
        NativeBytesStore[] nbs = {
                NativeBytesStore.nativeStoreWithFixedCapacity(140),
                NativeBytesStore.nativeStoreWithFixedCapacity(149),
                NativeBytesStore.nativeStoreWithFixedCapacity(159),
                NativeBytesStore.nativeStoreWithFixedCapacity(194)};
        Random rand = new Random();
        for (NativeBytesStore nb : nbs) {
            byte[] bytes = new byte[(int) nb.capacity()];
            rand.nextBytes(bytes);
            nb.write(0, bytes);
            assertEquals(Bytes.wrapForRead(bytes).byteCheckSum(), nb.byteCheckSum());
        }
        for (int t = 0; t < 3; t++) {
            int runs = 10000000;
            long start = System.nanoTime();
            for (int i = 0; i < runs; i += 4) {
                for (NativeBytesStore nb : nbs) {
                    bcs = nb.byteCheckSum();
                    if (bcs < 0 || bcs > 255)
                        throw new AssertionError();
                }
            }
            long time = System.nanoTime() - start;
            System.out.printf("Average time was %,d ns%n", time / runs);
        }
    }
}
