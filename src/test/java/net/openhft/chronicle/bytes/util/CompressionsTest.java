/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CompressionsTest {

    @Test
    @DisplayName("binary compression round trip restores original bytes")
    void testBinaryCompression() {
        byte[] original = "test data".getBytes(StandardCharsets.ISO_8859_1);

        byte[] compressed = Compressions.Binary.compress(original);
        byte[] decompressed = Compressions.Binary.uncompress(compressed);
        assertEquals(new String(original, StandardCharsets.ISO_8859_1),
                new String(decompressed, StandardCharsets.ISO_8859_1),
                "Binary compression should round trip the original bytes");

        InputStream decompressingStream = Compressions.Binary.decompressingStream(new ByteArrayInputStream(compressed));
        OutputStream compressingStream = Compressions.Binary.compressingStream(new ByteArrayOutputStream());
        assertNotNull(decompressingStream,
                "Binary decompression stream should be created for compressed input");
        assertNotNull(compressingStream,
                "Binary compression stream should be created for output buffering");
    }
}
