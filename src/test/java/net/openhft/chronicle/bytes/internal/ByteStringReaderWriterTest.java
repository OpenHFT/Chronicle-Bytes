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
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ByteStringReaderWriterTest extends BytesTestCommon {

    @Test
    public void readerReadsAllAndSkipHonoured() throws IOException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            bytes.append("abc123XYZ");

            final Reader reader = new ByteStringReader(bytes);

            // skip a few, then read remaining
            long skipped = reader.skip(3);
            assertEquals(3L, skipped);

            char[] buf = new char[16];
            int n = reader.read(buf, 0, buf.length);
            String s = new String(buf, 0, n);
            assertEquals("123XYZ", s);

            // EOF returns -1
            assertEquals(-1, reader.read());

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void writerAppendsVariousOverloads() throws IOException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            final ByteStringWriter writer = new ByteStringWriter(bytes);

            writer.write('A');
            writer.write("BC");
            writer.write("012345", 1, 3); // writes "123"
            Writer w = writer.append('X')
                    .append("YZ")
                    .append("-HELLO-", 1, 6); // "HELLO"
            w.flush();

            final String out = bytes.toString();
            assertTrue(out, out.contains("ABC123XYZHELLO"));
            assertEquals("ABC123XYZHELLO", out);

        } finally {
            bytes.releaseLast();
        }
    }
}

