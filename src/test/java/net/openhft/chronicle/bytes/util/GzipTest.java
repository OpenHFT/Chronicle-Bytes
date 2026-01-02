/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.util.Compressions.GZIP;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class GzipTest extends BytesTestCommon {

    @Test
    @DisplayName("gzip round trip preserves the original bytes")
    public void testCompress()
            throws IORuntimeException {
        @NotNull byte[] bytes = "hello world".getBytes(ISO_8859_1);
        byte[] bytes2 = GZIP.uncompress(GZIP.compress(bytes));
        assertArrayEquals(bytes,
                bytes2,
                "Gzip round trip should preserve the original bytes");
    }

    @Test
    @DisplayName("gzip compression ratio stays consistent across input types")
    public void testCompressionRatio()
            throws IORuntimeException {
        assumeFalse(NativeBytes.areNewGuarded(), "Gzip ratio test requires unguarded native bytes");
        @NotNull byte[] bytes = new byte[1 << 20];
        Arrays.fill(bytes, (byte) 'X');
        @NotNull Random rand = new Random();
        for (int i = 0; i < bytes.length; i += 40)
            bytes[rand.nextInt(bytes.length)] = '1';
        byte[] compress = GZIP.compress(bytes);

        Bytes<?> bytes2 = Bytes.wrapForRead(bytes);
        @NotNull Bytes<?> bytes3 = Bytes.allocateElasticDirect();
        GZIP.compress(bytes2, bytes3);
        @NotNull byte[] bytes4 = bytes3.toByteArray();
        byte[] bytes5 = GZIP.uncompress(bytes4);

        assertNotNull(bytes5,
                "Gzip uncompress should return a byte array");
        assertEquals(compress.length,
                bytes4.length,
                "Gzip compressed length should match Bytes output length");
        assertArrayEquals(compress,
                bytes4,
                "Gzip compressed bytes should match the direct output");

        @NotNull Bytes<?> bytes6 = Bytes.allocateElasticDirect();
        GZIP.uncompress(bytes3, bytes6);
        assertArrayEquals(bytes,
                bytes6.toByteArray(),
                "Gzip uncompress should restore the original payload");
        bytes2.releaseLast();
        bytes3.releaseLast();
        bytes6.releaseLast();
    }
}
