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

public class BytesInternalUtf8MoreTest extends BytesTestCommon {

    @Test
    public void appendUtf8WithLatin1MultibyteChars() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(64);
        try {
            String s = "ab\u00A3\u00E9cd"; // contains '£' and 'é'
            BytesInternal.appendUtf8(out, s, 0, s.length());
            // Bytes.toString decodes ISO-8859-1; compare using the same codec on the UTF-8 bytes
            String expected = new String(s.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.charset.StandardCharsets.ISO_8859_1);
            assertEquals(expected, out.toString());
        } finally {
            out.releaseLast();
        }
    }

    @Test
    public void parseUtf8IntoBytesBuilder() {
        Bytes<?> t1 = Bytes.from("token1");
        Bytes<?> t2 = Bytes.from("token2");
        Bytes<?> builder = Bytes.allocateElasticOnHeap(32);
        try {
            BytesInternal.parseUtf8(t1, builder, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("token1", builder.toString());
            builder.clear();
            BytesInternal.parseUtf8(t2, builder, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("token2", builder.toString());
        } finally {
            builder.releaseLast();
            t1.releaseLast();
            t2.releaseLast();
        }
    }
}
