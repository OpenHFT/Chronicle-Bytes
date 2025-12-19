/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("deprecation")
class CompressionsTest {

    @Test
    void testBinaryCompression() {
        byte[] original = "test data".getBytes(ISO_8859_1);

        byte[] compressed = Compressions.Binary.compress(original);
        byte[] decompressed = Compressions.Binary.uncompress(compressed);
        assertEquals(new String(original, ISO_8859_1), new String(decompressed, ISO_8859_1), "testBinaryCompression: assertEquals");

        InputStream decompressingStream = Compressions.Binary.decompressingStream(new ByteArrayInputStream(compressed));
        OutputStream compressingStream = Compressions.Binary.compressingStream(new ByteArrayOutputStream());
        assertNotNull(decompressingStream, "decompressingStream should be created successfully");
        assertNotNull(compressingStream, "compressingStream should be created successfully");
    }
}
