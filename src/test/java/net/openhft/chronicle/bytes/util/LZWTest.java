/*
 * Copyright 2016-2020 chronicle.software
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

package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.util.Compressions.LZW;
import static org.junit.Assert.*;

public class LZWTest extends BytesTestCommon {

    @Test
    public void testCompress()
            throws IORuntimeException {
        @NotNull byte[] bytes = "hello world".getBytes(ISO_8859_1);
        byte[] bytes2 = LZW.uncompress(LZW.compress(bytes));
        assertArrayEquals(bytes, bytes2);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testCompressionRatio()
            throws IORuntimeException {
        @NotNull byte[] bytes = new byte[1 << 20];
        Arrays.fill(bytes, (byte) 'X');
        @NotNull Random rand = new Random();
        for (int i = 0; i < bytes.length; i += 40)
            bytes[rand.nextInt(bytes.length)] = '1';
        byte[] compress = LZW.compress(bytes);
//        System.out.println(compress.length);

        Bytes bytes2 = Bytes.wrapForRead(bytes);
        @NotNull Bytes bytes3 = Bytes.allocateElasticDirect();
        LZW.compress(bytes2, bytes3);
        @NotNull byte[] bytes4 = bytes3.toByteArray();
        byte[] bytes5 = LZW.uncompress(bytes4);
        assertNotNull(bytes5);

//        assertEquals(Arrays.toString(bytes).replace(", ", "\n"),
//                Arrays.toString(bytes5).replace(", ", "\n"));
//        assertEquals(Arrays.toString(compress).replace(", ", "\n"),
//                Arrays.toString(bytes4).replace(", ", "\n"));
        assertEquals(compress.length, bytes4.length);
        assertArrayEquals(compress, bytes4);

        @NotNull Bytes bytes6 = Bytes.allocateElasticDirect();
        LZW.uncompress(bytes3, bytes6);
        assertArrayEquals(bytes, bytes6.toByteArray());
//        assertEquals(Arrays.toString(bytes).replace(", ", "\n"),
//                Arrays.toString(bytes6.toByteArray()).replace(", ", "\n"));
        bytes2.releaseLast();
        bytes3.releaseLast();
        bytes6.releaseLast();
    }
}