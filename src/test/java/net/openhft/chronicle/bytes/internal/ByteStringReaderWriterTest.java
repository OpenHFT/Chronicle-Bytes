/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ByteString reader and writer behaviour for stream style adapters")
public class ByteStringReaderWriterTest extends BytesTestCommon {

    @Test
    @DisplayName("reader reads all characters and honours skip positions")
    public void readerReadsAllAndSkipHonoured() throws IOException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            bytes.append("abc123XYZ");

            try (Reader reader = new ByteStringReader(bytes)) {
                // skip a few, then read remaining
                long skipped = reader.skip(3);
                assertEquals(3L, skipped,
                        "Reader skip returns expected count");

                char[] buf = new char[16];
                int n = reader.read(buf, 0, buf.length);
                String s = new String(buf, 0, n);
                assertEquals("123XYZ", s,
                        "Reader returns remaining characters");

                // EOF returns -1
                assertEquals(-1, reader.read(),
                        "Reader returns EOF after all data");
            }

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writer appends various overloads correctly for output sequences")
    public void writerAppendsVariousOverloads() throws IOException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            try (ByteStringWriter writer = new ByteStringWriter(bytes)) {
                writer.write('A');
                writer.write("BC");
                writer.write("012345", 1, 3); // writes "123"
                Writer w = writer.append('X')
                        .append("YZ")
                        .append("-HELLO-", 1, 6); // "HELLO"
                w.flush();

                final String out = bytes.toString();
                assertTrue(out.contains("ABC123XYZHELLO"),
                        "Writer output " + out + " contains ABC123XYZHELLO");
                assertEquals("ABC123XYZHELLO", out,
                        "Writer output matches expected sequence");
            }

        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writer wraps append failures as IOExceptions")
    public void writerWrapsIllegalStateAsIOException() throws IOException {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        ByteStringWriter writer = new ByteStringWriter(bytes);
        bytes.releaseLast();

        IOException exception = assertThrows(IOException.class,
                () -> writer.write('Z'),
                "Writer should wrap append failures as IOExceptions");
        assertNotNull(exception.getCause(),
                "IOException exposes the underlying cause");
        writer.close();
    }
}
