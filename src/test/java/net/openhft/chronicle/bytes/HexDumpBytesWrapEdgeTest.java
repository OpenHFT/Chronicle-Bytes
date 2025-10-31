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

import static org.junit.Assert.assertTrue;

public class HexDumpBytesWrapEdgeTest extends BytesTestCommon {

    @Test
    public void numberWrapOneSplitsEveryByte() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(1).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("wrap1");
            byte[] payload = new byte[5];
            hdb.write(payload);
            String s = hdb.toHexString();
            String[] lines = s.split("\\R");
            // 1 header + 5 data lines (wrapping every byte) + possibly a trailing empty line
            assertTrue("Expected multiple wrapped lines", lines.length >= 5);
            assertTrue(s.contains("wrap1"));
        } finally {
            hdb.releaseLast();
        }
    }
}

