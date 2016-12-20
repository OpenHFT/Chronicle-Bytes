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
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Test
    public void testElasticByteBuffer() throws IORuntimeException, BufferOverflowException {
        Bytes<ByteBuffer> bbb = Bytes.elasticByteBuffer();
        assertEquals(Bytes.MAX_CAPACITY, bbb.capacity());
        assertEquals(Bytes.DEFAULT_BYTE_BUFFER_CAPACITY, bbb.realCapacity());
        ByteBuffer bb = bbb.underlyingObject();
        assertNotNull(bb);

        for (int i = 0; i < 20; i++) {
            bbb.writeSkip(1000);
            bbb.writeLong(12345);
        }
        assertEquals(OS.pageSize() * 5, bbb.realCapacity());
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
    @Ignore("Long running test")
    public void perfCheckSum() throws IORuntimeException {
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

    @Test
    public void testCopyTo() {
        Bytes<ByteBuffer> src = Bytes.elasticByteBuffer().writeUtf8("hello");
        Bytes<ByteBuffer> dst = Bytes.elasticByteBuffer();

        dst.writePosition(src.copyTo(dst));
        assertEquals(src, dst);
    }
}
