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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.Test;

import static org.junit.Assert.*;

public class RandomDataInputUtf8LimitedTest extends BytesTestCommon {

    @Test
    public void nullSequenceEncodesAsMinusOneAndReturnsNegativeOffset() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            // writeStopBit(-1) then try to read with readUtf8Limited
            b.writeStopBit(-1);
            long res = b.readUtf8Limited(0, new StringBuilder(), 10);
            assertTrue("Expected negative return value signalling null", res < 0);
        } finally {
            b.releaseLast();
        }
    }

    @Test(expected = ClosedIllegalStateException.class)
    public void throwsWhenUtf8LengthExceedsMax() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            // payload of length 4, but allow only 3
            String payload = "WXYZ";
            // stop-bit length then the bytes
            b.writeStopBit(AppendableUtil.findUtf8Length(payload));
            b.append(payload);

            StringBuilder sb = new StringBuilder();
            b.readUtf8Limited(0, sb, 3);
        } finally {
            b.releaseLast();
        }
    }
}

