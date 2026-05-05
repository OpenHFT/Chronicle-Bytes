/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests streaming input stream behaviour because correct byte ordering
 * and zero-length read semantics are essential for interoperability with
 * standard Java I/O consumers.
 */
@DisplayName("StreamingInputStream - validates InputStream adapter behaviour")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class StreamingInputStreamTest extends BytesTestCommon {

    // https://github.com/OpenHFT/Chronicle-Bytes/issues/48
    @Test
    @DisplayName("read with zero length returns zero bytes immediately")
    public void readOfZeroShouldReturnZero()
            throws IOException {
        @NotNull Bytes<?> b = Bytes.allocateElasticDirect();
        prepareBytes(b);

        @NotNull InputStream is = b.inputStream();
        try {
            assertEquals(0,
                    is.read(new byte[5], 0, 0),
                    "Zero-length read should return zero");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @Timeout(1)
    @DisplayName("input stream reads all bytes in order")
    public void testReadBlock()
            throws IOException {

        @NotNull Bytes<?> b = Bytes.allocateElasticDirect();
        @NotNull byte[] test = prepareBytes(b);

        @NotNull InputStream is = b.inputStream();
        try (@NotNull ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            @NotNull byte[] buffer = new byte[8];
            for (int len; (len = is.read(buffer)) != -1; )
                os.write(buffer, 0, len);
            os.flush();
            assertArrayEquals(test,
                    os.toByteArray(),
                    "Input stream should read the same bytes that were written");
        } finally {
            b.releaseLast();
        }
    }

    private byte[] prepareBytes(final Bytes<?> b) {
        @NotNull byte[] test = "Hello World, Have a great day!".getBytes(ISO_8859_1);
        b.write(test);
        return test;
    }
}
