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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesInternalParseUtf8BoundaryTest extends BytesTestCommon {

    @Test
    public void parsesExplicitLengthAtBoundaries() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        String ascii = "A";
        String multi = "£€"; // multi-byte UTF-8 in literal is fine
        try {
            b.append(ascii).append(multi);
            b.readPosition(0);

            StringBuilder sb = new StringBuilder();
            // parse up to the first byte (1 char)
            BytesInternal.parseUtf8(b, sb, true, 1);
            assertEquals(ascii, sb.toString());

            sb.setLength(0);
            // parse remaining (multi-byte sequence)
            BytesInternal.parseUtf8(b, sb, true, (int) b.readRemaining());
            assertEquals(multi, sb.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void parsesWithStopCharsTester() {
        Bytes<?> b = Bytes.from("alpha,beta gamma");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(b, sb, StopCharTesters.COMMA_STOP);
            assertEquals("alpha", sb.toString());
        } finally {
            b.releaseLast();
        }
    }
}

