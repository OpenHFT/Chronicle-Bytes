/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue523Test extends BytesTestCommon {

    @SuppressWarnings("EmptyMethod")
    @BeforeEach
    @Override
    public void threadDump() {
        super.threadDump();
    }

    @Test
    public void testAppendDoublesHeap() {
        Set<String> failures = doTestAppendDoubles(Bytes::allocateElasticOnHeap);
        assertEquals(0, failures.size(), "testAppendDoublesHeap: failures=" + failures);
    }

    @Test
    public void testAppendDoublesHeapByteBuffer() {
        Set<String> failures = doTestAppendDoubles(Bytes::elasticHeapByteBuffer);
        assertEquals(0, failures.size(), "testAppendDoublesHeapByteBuffer: failures=" + failures);
    }

    @Test
    public void testAppendDoublesDirect() {
        Set<String> failures = doTestAppendDoubles(Bytes::allocateElasticDirect);
        assertEquals(0, failures.size(), "testAppendDoublesDirect: failures=" + failures);
    }

    private Set<String> doTestAppendDoubles(Supplier<Bytes<?>> bytesSupplier) {
        Set<String> collect = IntStream.range(0, 1000)
                .parallel()
                .mapToObj(i -> {
                    Bytes<?> bytes = bytesSupplier.get();
                    try {
                        double ee = 1e6;
                        for (int e = 6; e <= 12; e++) {
                            for (int j = 0; j < 1000; j++) {
                                int k = i + j * 1000;
                                double d = k / ee;
                                bytes.clear();
                                bytes.append(d);
                                int l = (int) bytes.readRemaining();
                                if (l > 2 + e)
                                    return d + ": " + bytes + " too long";
                                if (bytes.parseDouble() != d) {
                                    bytes.readPosition(0);
                                    return d + " != " + bytes;
                                }
                            }
                            ee *= 10;
                        }
                    } finally {
                        bytes.releaseLast();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        if (!collect.isEmpty())
            System.out.println(collect);
        return collect;
    }
}
