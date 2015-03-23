/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Test;

public class ByteStringAppenderTest {

    @Test
    public void testAppend() throws Exception {

        Bytes bytes = Bytes.elasticByteBuffer();
        long expected = 1234;
        bytes.append(expected);
        bytes.flip();
        Assert.assertEquals(expected, bytes.parseLong());
    }

    @Test
    public void testAppendWithOffset() throws Exception {
        Bytes bytes = Bytes.elasticByteBuffer();
        bytes.limit(20);
        for (long expected : new long[]{123456, 12345, 1234, 123, 12, 1, 0}) {
            bytes.append(10, expected, 6);
            Assert.assertEquals(expected, bytes.parseLong(10));
        }
    }

    @Test
    public void testAppendWithOffsetNeg() throws Exception {
        Bytes bytes = Bytes.elasticByteBuffer();
        bytes.limit(20);
        for (long expected : new long[]{-123456, 12345, -1234, 123, -12, 1, 0}) {
            bytes.append(10, expected, 7);
            Assert.assertEquals(expected, bytes.parseLong(10));
        }
    }


}