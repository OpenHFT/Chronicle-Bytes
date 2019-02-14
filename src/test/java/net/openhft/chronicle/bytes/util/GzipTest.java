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

package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.threads.ThreadDump;
import net.openhft.chronicle.core.annotation.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.util.Compressions.GZIP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GzipTest {

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
    public void testCompress() throws IORuntimeException {
        @NotNull byte[] bytes = "hello world".getBytes(ISO_8859_1);
        byte[] bytes2 = GZIP.uncompress(GZIP.compress(bytes));
        assertTrue(Arrays.equals(bytes, bytes2));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCompressionRatio() throws IORuntimeException {
        @NotNull byte[] bytes = new byte[1 << 20];
        Arrays.fill(bytes, (byte) 'X');
        @NotNull Random rand = new Random();
        for (int i = 0; i < bytes.length; i += 40)
            bytes[rand.nextInt(bytes.length)] = '1';
        byte[] compress = GZIP.compress(bytes);
        System.out.println(compress.length);

        Bytes bytes2 = Bytes.wrapForRead(bytes);
        @NotNull Bytes bytes3 = Bytes.allocateElasticDirect();
        GZIP.compress(bytes2, bytes3);
        @NotNull byte[] bytes4 = bytes3.toByteArray();
        byte[] bytes5 = GZIP.uncompress(bytes4);

        assertNotNull(bytes5);
//        assertEquals(Arrays.toString(bytes).replace(", ", "\n"),
//                Arrays.toString(bytes5).replace(", ", "\n"));
//        assertEquals(Arrays.toString(compress).replace(", ", "\n"),
//                Arrays.toString(bytes4).replace(", ", "\n"));
        assertEquals(compress.length, bytes4.length);
        assertTrue(Arrays.equals(compress, bytes4));

        @NotNull Bytes bytes6 = Bytes.allocateElasticDirect();
        GZIP.uncompress(bytes3, bytes6);
        assertTrue(Arrays.equals(bytes, bytes6.toByteArray()));
//        assertEquals(Arrays.toString(bytes).replace(", ", "\n"),
//                Arrays.toString(bytes6.toByteArray()).replace(", ", "\n"));
    }
}