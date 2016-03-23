/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
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

    @Test
    public void testCompress() {
        byte[] bytes = "hello world".getBytes();
        byte[] bytes2 = LZW.uncompress(LZW.compress(bytes));
        assertTrue(Arrays.equals(bytes, bytes2));
    }

    @Test
    public void testCompressionRatio() {
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