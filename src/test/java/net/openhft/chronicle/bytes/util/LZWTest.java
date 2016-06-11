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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static net.openhft.chronicle.bytes.util.Compressions.LZW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by peter.lawrey on 09/12/2015.
 */
public class LZWTest {

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
        byte[] bytes = "hello world".getBytes();
        byte[] bytes2 = LZW.uncompress(LZW.compress(bytes));
        assertTrue(Arrays.equals(bytes, bytes2));
    }

    @Test
    public void testCompressionRatio() throws IORuntimeException {
        byte[] bytes = new byte[1 << 20];
        Arrays.fill(bytes, (byte) 'X');
        Random rand = new Random();
        for (int i = 0; i < bytes.length; i += 40)
            bytes[rand.nextInt(bytes.length)] = '1';
        byte[] compress = LZW.compress(bytes);
        System.out.println(compress.length);

        Bytes bytes2 = Bytes.wrapForRead(bytes);
        Bytes bytes3 = Bytes.allocateElasticDirect();
        LZW.compress(bytes2, bytes3);
        byte[] bytes4 = bytes3.toByteArray();
        byte[] bytes5 = LZW.uncompress(bytes4);

//        assertEquals(Arrays.toString(bytes).replace(", ", "\n"),
//                Arrays.toString(bytes5).replace(", ", "\n"));
//        assertEquals(Arrays.toString(compress).replace(", ", "\n"),
//                Arrays.toString(bytes4).replace(", ", "\n"));
        assertEquals(compress.length, bytes4.length);
        assertTrue(Arrays.equals(compress, bytes4));

        Bytes bytes6 = Bytes.allocateElasticDirect();
        LZW.uncompress(bytes3, bytes6);
        assertTrue(Arrays.equals(bytes, bytes6.toByteArray()));
//        assertEquals(Arrays.toString(bytes).replace(", ", "\n"),
//                Arrays.toString(bytes6.toByteArray()).replace(", ", "\n"));
    }
}