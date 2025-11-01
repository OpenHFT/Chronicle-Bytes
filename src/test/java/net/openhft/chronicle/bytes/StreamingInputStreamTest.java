/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StreamingInputStreamTest extends BytesTestCommon {

    // https://github.com/OpenHFT/Chronicle-Bytes/issues/48
    @Test
    public void readOfZeroShouldReturnZero()
            throws IOException {
        @NotNull Bytes<?> b = Bytes.allocateElasticDirect();
        prepareBytes(b);

        @NotNull InputStream is = b.inputStream();
        assertEquals(0, is.read(new byte[5], 0, 0));
        b.releaseLast();
    }

    @Test(timeout = 1000)
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
            assertArrayEquals(test, os.toByteArray());
        }

        b.releaseLast();
    }

    private byte[] prepareBytes(final Bytes<?> b) {
        @NotNull byte[] test = "Hello World, Have a great day!".getBytes(ISO_8859_1);
        b.write(test);
        return test;
    }
}
