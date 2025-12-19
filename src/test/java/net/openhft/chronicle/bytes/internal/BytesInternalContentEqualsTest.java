/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BytesInternalContentEqualsTest extends BytesTestCommon {
    public static Collection<Object[]> data() {
        List<Object[]> tests = new ArrayList<>(Arrays.asList(new Object[][]{
                {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()},
                {Bytes.elasticHeapByteBuffer(), Bytes.elasticHeapByteBuffer()}
        }));
        if (Jvm.maxDirectMemory() > 0) {
            tests.addAll(Arrays.asList(new Object[][]{
                    {Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()},
                    {Bytes.elasticByteBuffer(), Bytes.elasticByteBuffer()},
                    {Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()},
                    {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()},
                    {Bytes.elasticHeapByteBuffer(), Bytes.elasticByteBuffer()}
            }));
        }
        return tests;
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testContentEqual(Bytes<?> left, Bytes<?> right) {
        left.clear();
        right.clear();
        try {
            left.append("hello world");
            right.append("hello world");
            assertTrue(left.contentEquals(right), "left.contentEquals");
        } finally {
            right.releaseLast();
            left.releaseLast();
        }
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testContentNotEqualButSameLen(Bytes<?> left, Bytes<?> right) {
        left.clear();
        right.clear();
        try {
            left.append("hello world1");
            right.append("hello world2");
            assertFalse(left.contentEquals(right), "left.contentEquals");
        } finally {
            right.releaseLast();
            left.releaseLast();
        }
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testContentNotEqualButDiffLen(Bytes<?> left, Bytes<?> right) {
        left.clear();
        right.clear();
        try {
            left.append("hello world");
            right.append("hello world2");
            assertFalse(left.contentEquals(right), "left.contentEquals");
        } finally {
            right.releaseLast();
            left.releaseLast();
        }
    }
}
