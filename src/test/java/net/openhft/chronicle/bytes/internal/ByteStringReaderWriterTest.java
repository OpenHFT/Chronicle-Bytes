/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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

