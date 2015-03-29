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

public class ByteStringParserTest   {

    @Test
    public void testParseLong() throws Exception {
        Bytes b = Bytes.elasticByteBuffer();
        long expected = 123L;
        b.append(expected);
        b.flip();
        Assert.assertEquals(expected,BytesUtil.parseLong(b));
    }

    @Test
    public void testParseInt() throws Exception {
        Bytes b = Bytes.elasticByteBuffer();
        int expected = 123;
        b.append(expected);
        b.flip();
        Assert.assertEquals(expected,BytesUtil.parseLong(b));
    }

    @Test
    public void testParseDouble() throws Exception {
        Bytes b = Bytes.elasticByteBuffer();
        double expected = 123.1234;
        b.append(expected);
        b.flip();
        Assert.assertEquals(expected,BytesUtil.parseDouble(b));
    }

    @Test
    public void testParseFloat() throws Exception {
        Bytes b = Bytes.elasticByteBuffer();
        float expected = 123;
        b.append(expected);
        b.flip();
        Assert.assertEquals(expected,BytesUtil.parseDouble(b));
    }

    @Test
    public void testParseShort() throws Exception {
        Bytes b = Bytes.elasticByteBuffer();
        short expected = 123;
        b.append(expected);
        b.flip();
        Assert.assertEquals(expected,BytesUtil.parseLong(b));
    }
}