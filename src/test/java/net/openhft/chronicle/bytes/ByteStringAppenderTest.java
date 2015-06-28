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

import org.junit.Assert;
import org.junit.Test;

public class ByteStringAppenderTest {

    @Test
    public void testAppend() {

        Bytes bytes = Bytes.elasticByteBuffer();
        long expected = 1234;
        bytes.append(expected);

        Assert.assertEquals(expected, bytes.parseLong());
    }

    @Test
    public void testAppendWithOffset() {
        Bytes bytes = Bytes.elasticByteBuffer();
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{123456, 12345, 1234, 123, 12, 1, 0}) {
            bytes.append(10, expected, 6);
            Assert.assertEquals(expected, bytes.parseLong(10));
        }
    }

    @Test
    public void testAppendWithOffsetNeg() {
        Bytes bytes = Bytes.elasticByteBuffer();
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{-123456, 12345, -1234, 123, -12, 1, 0}) {
            bytes.append(10, expected, 7);
            Assert.assertEquals(expected, bytes.parseLong(10));
        }
    }
}