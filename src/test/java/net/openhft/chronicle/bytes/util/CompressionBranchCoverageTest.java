/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for Compression to achieve branch coverage in the static
 * compress and uncompress methods.
 */
@DisplayName("Compression branch coverage for compress and uncompress fallbacks")
class CompressionBranchCoverageTest extends BytesTestCommon {

    // ========== compress() - G-prefixed unknown algorithms ==========

    @Test
    @DisplayName("compress with unknown G-prefixed algorithm falls back to binary")
    void compressFallsBackForUnknownGPrefix() {
        Bytes<?> uncompressed = Bytes.from("g-prefix payload");
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(64);
        try {
            Compression.compress("gz", uncompressed, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            assertEquals("g-prefix payload",
                    new String(compressed.toByteArray(), ISO_8859_1),
                    "Unknown G-prefixed algorithm should fall back to binary compression");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
        }
    }

    // ========== uncompress(BytesIn, BytesOut) - L-prefixed unknown ==========

    @Test
    @DisplayName("uncompress with unknown L-prefixed algorithm does nothing")
    void uncompressIgnoresNonLzwPrefix() {
        Bytes<?> input = Bytes.from("payload");
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            input.readPosition(0);
            input.readLimit(input.writePosition());
            Compression.uncompress("lzo", input, output);
            assertEquals(0, output.writePosition(),
                    "Unknown L-prefixed algorithm should leave output unchanged");
        } finally {
            input.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("uncompress with unknown G-prefixed algorithm does nothing")
    void uncompressIgnoresNonGzipPrefix() {
        Bytes<?> input = Bytes.from("payload");
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            input.readPosition(0);
            input.readLimit(input.writePosition());
            Compression.uncompress("gz", input, output);
            assertEquals(0, output.writePosition(),
                    "Unknown G-prefixed algorithm should leave output unchanged");
        } finally {
            input.releaseLast();
            output.releaseLast();
        }
    }

    // ========== uncompress(CharSequence, T, ThrowingFunction) - Additional branches ==========

    @Test
    @DisplayName("byte array uncompress with !binary alias returns data")
    void uncompressFunctionBangBinaryReturnsData() {
        byte[] data = "inputData".getBytes(ISO_8859_1);
        byte[] result = Compression.uncompress("!binary", data, ignored -> data);
        assertArrayEquals(data, result,
                "!binary uncompress should return the source bytes");
    }

    @Test
    @DisplayName("byte array uncompress with lzw processes compressed data")
    void uncompressFunctionLzwProcessesData() {
        byte[] original = "lzw test data".getBytes(ISO_8859_1);
        byte[] compressed = Compressions.LZW.compress(original);
        byte[] result = Compression.uncompress("lzw", compressed, identity -> identity);
        assertArrayEquals(original, result,
                "lzw uncompress should decompress the data");
    }

    @Test
    @DisplayName("byte array uncompress with gzip processes compressed data")
    void uncompressFunctionGzipProcessesData() {
        byte[] original = "gzip test data".getBytes(ISO_8859_1);
        byte[] compressed = Compressions.GZIP.compress(original);
        byte[] result = Compression.uncompress("gzip", compressed, identity -> identity);
        assertArrayEquals(original, result,
                "gzip uncompress should decompress the data");
    }

    @Test
    @DisplayName("byte array uncompress with unknown G-prefixed returns null")
    void uncompressFunctionGPrefixedReturnsNull() {
        byte[] data = "inputData".getBytes(ISO_8859_1);
        byte[] result = Compression.uncompress("gz", data, ignored -> data);
        assertNull(result,
                "Unknown G-prefixed compression should return null");
    }

    @Test
    @DisplayName("byte array uncompress with unknown !-prefixed returns null")
    void uncompressFunctionBangPrefixedUnknownReturnsNull() {
        byte[] data = "inputData".getBytes(ISO_8859_1);
        byte[] result = Compression.uncompress("!unknown", data, ignored -> data);
        assertNull(result,
                "Unknown !-prefixed compression should return null");
    }

    // ========== available() method ==========

    @Test
    @DisplayName("Binary compression availability reports true in this environment")
    void binaryCompressionIsAvailable() {
        assertTrue(Compressions.Binary.available(),
                "Binary compression should be available");
    }

    @Test
    @DisplayName("LZW compression availability reports true in this environment")
    void lzwCompressionIsAvailable() {
        assertTrue(Compressions.LZW.available(),
                "LZW compression should be available");
    }

    @Test
    @DisplayName("GZIP compression availability reports true in this environment")
    void gzipCompressionIsAvailable() {
        assertTrue(Compressions.GZIP.available(),
                "GZIP compression should be available");
    }

    // ========== Edge cases ==========

    @Test
    @DisplayName("compress empty bytes results in empty compressed output")
    void compressEmptyBytes() {
        Bytes<?> uncompressed = Bytes.allocateElasticOnHeap();
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(64);
        try {
            Compression.compress("binary", uncompressed, compressed);
            assertEquals(0, compressed.writePosition(),
                    "Compressing empty bytes should result in empty output");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
        }
    }

    @Test
    @DisplayName("uncompress empty binary bytes results in empty output")
    void uncompressEmptyBytes() {
        Bytes<?> input = Bytes.allocateElasticOnHeap();
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            Compression.uncompress("binary", input, output);
            assertEquals(0, output.writePosition(),
                    "Uncompressing empty bytes should result in empty output");
        } finally {
            input.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("byte array compress and uncompress via default methods")
    void byteArrayCompressUncompressRoundTrip() {
        byte[] original = "default method test".getBytes(ISO_8859_1);
        byte[] compressed = Compressions.Binary.compress(original);
        byte[] uncompressed = Compressions.Binary.uncompress(compressed);
        assertArrayEquals(original, uncompressed,
                "Byte array compress/uncompress should round-trip correctly");
    }

    @Test
    @DisplayName("lzw byte array compress and uncompress via ThrowingFunction")
    void lzwByteArrayRoundTripViaFunction() {
        byte[] original = "lzw function test".getBytes(ISO_8859_1);
        byte[] compressed = Compressions.LZW.compress(original);
        byte[] result = Compression.uncompress("lzw", compressed, identity -> identity);
        assertArrayEquals(original, result,
                "LZW byte array should round-trip correctly via function");
    }

    @Test
    @DisplayName("gzip byte array compress and uncompress via ThrowingFunction")
    void gzipByteArrayRoundTripViaFunction() {
        byte[] original = "gzip function test".getBytes(ISO_8859_1);
        byte[] compressed = Compressions.GZIP.compress(original);
        byte[] result = Compression.uncompress("gzip", compressed, identity -> identity);
        assertArrayEquals(original, result,
                "GZIP byte array should round-trip correctly via function");
    }
}
