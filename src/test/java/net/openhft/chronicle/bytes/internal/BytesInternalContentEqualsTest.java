/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class BytesInternalContentEqualsTest extends BytesTestCommon {
    private final Bytes<?> a;
    private final Bytes<?> b;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> tests = new ArrayList<>(Arrays.asList(new Object[][]{
                {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()}
                , {Bytes.elasticHeapByteBuffer(), Bytes.elasticHeapByteBuffer()}
        }));
        if (Jvm.maxDirectMemory() > 0) {
            tests.addAll(Arrays.asList(new Object[][]{
                    {Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()}
                    , {Bytes.elasticByteBuffer(), Bytes.elasticByteBuffer()}
                    , {Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()}
                    , {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()}
                    , {Bytes.elasticHeapByteBuffer(), Bytes.elasticByteBuffer()}
            }));
        }
        return tests;
    }

    public BytesInternalContentEqualsTest(Bytes<?> left, Bytes<?> right) {
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
        a.append("hello world1");
        b.append("hello world2");
        Assert.assertFalse(a.contentEquals(b));
    }

    @Test
    public void testContentNotEqualButDiffLen() {
        a.append("hello world");
        b.append("hello world2");
        Assert.assertFalse(a.contentEquals(b));
    }
}
