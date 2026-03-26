/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class BytesInternalContentEqualsTest extends BytesTestCommon {

    static Stream<Arguments> data() {
        List<Arguments> tests = new ArrayList<>(Arrays.asList(
                Arguments.of(Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()),
                Arguments.of(Bytes.elasticHeapByteBuffer(), Bytes.elasticHeapByteBuffer())
        ));
        if (Jvm.maxDirectMemory() > 0) {
            tests.addAll(Arrays.asList(
                    Arguments.of(Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()),
                    Arguments.of(Bytes.elasticByteBuffer(), Bytes.elasticByteBuffer()),
                    Arguments.of(Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()),
                    Arguments.of(Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()),
                    Arguments.of(Bytes.elasticHeapByteBuffer(), Bytes.elasticByteBuffer())
            ));
        }
        return tests.stream();
    }

    @ParameterizedTest
    @MethodSource("data")
    void testContentEqual(Bytes<?> a, Bytes<?> b) {
        a.clear();
        b.clear();
        a.append("hello world");
        b.append("hello world");
        assertTrue(a.contentEquals(b));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testContentNotEqualButSameLen(Bytes<?> a, Bytes<?> b) {
        a.clear();
        b.clear();
        a.append("hello world1");
        b.append("hello world2");
        assertFalse(a.contentEquals(b));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testContentNotEqualButDiffLen(Bytes<?> a, Bytes<?> b) {
        a.clear();
        b.clear();
        a.append("hello world");
        b.append("hello world2");
        assertFalse(a.contentEquals(b));
    }
}
