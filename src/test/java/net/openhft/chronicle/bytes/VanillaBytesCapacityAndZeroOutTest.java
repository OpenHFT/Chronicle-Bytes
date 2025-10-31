/*
 * Copyright 2016-2025 chronicle.software
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

import static org.junit.Assert.*;

public class VanillaBytesCapacityAndZeroOutTest extends BytesTestCommon {

    @Test
    public void ensureCapacityGrowsAndZeroOutsRange() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            // Grow in small steps
            for (int i = 0; i < 10; i++) {
                b.append('X');
            }
            long capAfter = b.capacity();
            assertTrue("Expected capacity to grow beyond initial", capAfter >= 10);

            // zeroOut a large range including unwritten tail
            long start = 2;
            long end = Math.min(b.writePosition() + 16, b.capacity());
            b.zeroOut(start, end);

            // Verify visible zeroing only on written region
            b.readPosition(0);
            byte first = b.readByte();
            assertEquals('X', first);
            byte third = b.readByte(2);
            assertEquals(0, third);
        } finally {
            b.releaseLast();
        }
    }
}

