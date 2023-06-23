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
package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class Issue523Test {
    @Test
    public void testAppendDoublesHeap() {
        doTestAppendDoubles(Bytes::allocateElasticOnHeap);
    }

    @Test
    public void testAppendDoublesHeapByteBuffer() {
        doTestAppendDoubles(Bytes::elasticHeapByteBuffer);
    }

    @Test
    public void testAppendDoublesDirect() {
        doTestAppendDoubles(Bytes::allocateElasticDirect);
    }

    public void doTestAppendDoubles(Supplier<Bytes> bytesSupplier) {
        Set<String> collect = IntStream.range(0, 1000)
                .parallel()
                .mapToObj(i -> {
                    Bytes bytes = bytesSupplier.get();
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
/*
                                for (int ii = 0; ii < l; ii++)
                                    if ("0123456789.".indexOf(bytes.readByte(ii)) < 0)
                                        return d + ": " + bytes + " notation";
*/
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
        System.out.println(collect);
        assertEquals(0, collect.size());
    }
}
