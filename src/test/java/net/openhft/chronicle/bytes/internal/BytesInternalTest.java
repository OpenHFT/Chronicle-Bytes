/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class BytesInternalTest extends BytesTestCommon {
    private final Bytes a;
    private final Bytes b;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()}
                , {Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()}
                , {Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()}
                , {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()}
                , {Bytes.elasticByteBuffer(), Bytes.elasticByteBuffer()}
                , {Bytes.elasticHeapByteBuffer(), Bytes.elasticHeapByteBuffer()}
                , {Bytes.elasticHeapByteBuffer(), Bytes.elasticByteBuffer()}
        });
    }

    public BytesInternalTest(Bytes left, Bytes right) {
        this.a = left;
        this.b = right;
    }

    @Before
    public void before() {
        a.clear();
        b.clear();

    }

    @Test
    public void testContentEqual() {
        a.append("hello world");
        b.append("hello world");
        Assert.assertTrue(a.contentEquals(b));
    }

    @Test
    public void testContentNotEqualButSameLen() {
        a.append("hello world1111");
        b.append("hello world1112");
        Assert.assertFalse(a.contentEquals(b));
    }

    @Test
    public void testContentNotEqualButDiffLen() {
        a.append("hello world");
        b.append("hello world2");
        Assert.assertFalse(a.contentEquals(b));
    }


}