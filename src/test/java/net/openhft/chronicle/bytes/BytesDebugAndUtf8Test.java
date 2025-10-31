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
import static org.junit.Assert.assertTrue;

public class BytesDebugAndUtf8Test extends BytesTestCommon {

    @Test
    public void appendAndParseUtf8AndDebugString() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        try {
            BytesUtil.appendUtf8(b, "hello");
            long rp = b.readPosition();
            StringBuilder sb = new StringBuilder();
            BytesUtil.parseUtf8(b, sb, 5);
            assertEquals("hello", sb.toString());

            // debug string contains representation
            String dbg = BytesUtil.toDebugString(b, rp, 5);
            assertTrue(dbg.length() > 0);
        } finally {
            b.releaseLast();
        }
    }
}

