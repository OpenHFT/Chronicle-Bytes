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

import static org.junit.Assert.assertEquals;

public class BytesWrite8bitRoundTripTest extends BytesTestCommon {

    @Test
    public void roundTripOnHeap() {
        roundTrip(Bytes.allocateElasticOnHeap());
    }

    @Test
    public void roundTripDirect() {
        roundTrip(Bytes.allocateElasticDirect());
    }

    private void roundTrip(Bytes<?> bytes) {
        try {
            String[] names = {
                    "", // empty
                    "a",
                    "helloWorld",
                    // ISO-8859-1 content
                    "price£",
                    // near 255 boundary
                    repeat('x', 250)
            };

            for (String s : names) {
                long pos0 = bytes.writePosition();
                bytes.write8bit(s);
                long pos1 = bytes.writePosition();
                bytes.readPosition(pos0);
                String got = bytes.read8bit();
                assertEquals(s, got);
                // read position should catch up to write
                assertEquals(pos1, bytes.readPosition());
            }
        } finally {
            bytes.releaseLast();
        }
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}

