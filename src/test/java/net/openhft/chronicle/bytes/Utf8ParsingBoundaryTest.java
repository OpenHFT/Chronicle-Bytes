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

import net.openhft.chronicle.bytes.internal.BytesInternal;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Consolidates UTF-8 parsing boundary tests (explicit length, stop-char parsing,
 * null sequences and over-length failures).
 */
public class Utf8ParsingBoundaryTest extends BytesTestCommon {

    @Test
    public void parsesExplicitLengthAtBoundaries() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        String ascii = "A";
        String multi = "£\u20ac"; // euro escaped; pound is ISO-8859-1
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
    public void parsesWithCommonStopChars() {
        Bytes<?> b = Bytes.from("alpha,beta gamma");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(b, sb, StopCharTesters.COMMA_STOP);
            assertEquals("alpha", sb.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void nullSequenceEncodesAsMinusOneAndReturnsNegativeOffset() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            b.writeStopBit(-1);
            long res = b.readUtf8Limited(0, new StringBuilder(), 10);
            assertTrue("Expected negative return value signalling null", res < 0);
        } finally {
            b.releaseLast();
        }
    }

    @Test(expected = net.openhft.chronicle.core.io.ClosedIllegalStateException.class)
    public void throwsWhenUtf8LengthExceedsMax() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            String payload = "WXYZ";
            b.writeStopBit(AppendableUtil.findUtf8Length(payload));
            b.append(payload);
            StringBuilder sb = new StringBuilder();
            b.readUtf8Limited(0, sb, 3);
        } finally {
            b.releaseLast();
        }
    }
}

