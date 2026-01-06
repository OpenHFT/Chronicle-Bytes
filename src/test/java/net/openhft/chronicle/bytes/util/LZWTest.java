/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.util.Compressions.LZW;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("deprecation")
public class LZWTest extends BytesTestCommon {

    @Test
    @DisplayName("lzw round trip preserves the original bytes")
    public void testCompress()
            throws IORuntimeException {
        @NotNull byte[] bytes = "hello world".getBytes(ISO_8859_1);
        byte[] bytes2 = LZW.uncompress(LZW.compress(bytes));
        assertArrayEquals(bytes,
                bytes2,
                "LZW round trip should preserve the original bytes");
    }

    @Test
    @DisplayName("lzw compression output matches Bytes-based pipeline")
    public void testCompressionRatio()
            throws IORuntimeException {
        @NotNull byte[] bytes = new byte[1 << 20];
        Arrays.fill(bytes, (byte) 'X');
        @NotNull Random rand = new Random();
        for (int i = 0; i < bytes.length; i += 40)
            bytes[rand.nextInt(bytes.length)] = '1';
        byte[] compress = LZW.compress(bytes);

        Bytes<?> bytes2 = Bytes.wrapForRead(bytes);
        @NotNull Bytes<?> bytes3 = Bytes.allocateElasticDirect();
        LZW.compress(bytes2, bytes3);
        @NotNull byte[] bytes4 = bytes3.toByteArray();
        byte[] bytes5 = LZW.uncompress(bytes4);
        assertNotNull(bytes5,
                "LZW uncompress should return a byte array");

        assertEquals(compress.length,
                bytes4.length,
                "LZW compressed length should match Bytes output length");
        assertArrayEquals(compress,
                bytes4,
                "LZW compressed bytes should match the direct output");

        @NotNull Bytes<?> bytes6 = Bytes.allocateElasticDirect();
        LZW.uncompress(bytes3, bytes6);
        assertArrayEquals(bytes,
                bytes6.toByteArray(),
                "LZW uncompress should restore the original payload");
        bytes2.releaseLast();
        bytes3.releaseLast();
        bytes6.releaseLast();
    }
}
