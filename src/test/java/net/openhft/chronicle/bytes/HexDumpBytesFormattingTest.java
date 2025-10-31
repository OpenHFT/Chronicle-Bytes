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

public class HexDumpBytesFormattingTest extends BytesTestCommon {

    @Test
    public void wrapsOffsetsAndNestedBlocks() {
        HexDumpBytes hdb = new HexDumpBytes();
        try {
            hdb.numberWrap(8).offsetFormat((o, b) -> b.appendBase16(o, 2));
            hdb.writeHexDumpDescription("hdr");
            hdb.write(new byte[32]);
            hdb.adjustHexDumpIndentation(1);
            hdb.writeHexDumpDescription("nested");
            hdb.write(new byte[4]);
            String s = hdb.toHexString();
            assertTrue(s.contains("hdr"));
            assertTrue(s.contains("nested"));
            assertTrue(s.contains("00"));
        } finally {
            hdb.releaseLast();
        }
    }
}

