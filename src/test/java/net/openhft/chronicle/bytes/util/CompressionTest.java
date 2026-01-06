/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("deprecation")
public class CompressionTest {

    @Test
    @DisplayName("unsupported algorithm falls back to binary compression")
    public void compressFallsBackToBinary() {
        Bytes<?> uncompressed = Bytes.from("payload");
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(64);
        try {
            Compression.compress("unsupported_algo", uncompressed, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            assertEquals("payload",
                    new String(compressed.toByteArray(), ISO_8859_1),
                    "Binary fallback should retain the original payload bytes");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
        }
    }

    @Test
    @DisplayName("L-prefixed unknown algorithms still fall back to binary compression")
    public void compressFallsBackForUnknownLPrefix() {
        Bytes<?> uncompressed = Bytes.from("l-prefix payload");
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(64);
        try {
            Compression.compress("lzo", uncompressed, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            assertEquals("l-prefix payload",
                    new String(compressed.toByteArray(), ISO_8859_1),
                    "Unknown L-prefixed algorithm should fall back to binary compression");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
        }
    }

    @Test
    @DisplayName("binary and !binary uncompress to the original bytes")
    public void uncompressBinaryAliasRestoresOriginal() {
        Bytes<?> uncompressed = Bytes.from("binaryPayload");
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(64);
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            Compression.compress("binary", uncompressed, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            Compression.uncompress("!binary", compressed, output);
            output.readPosition(0);
            output.readLimit(output.writePosition());
            assertEquals("binaryPayload",
                    new String(output.toByteArray(), ISO_8859_1),
                    "Binary alias should restore the original payload bytes");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("uncompress rejects unsupported algorithm with IllegalArgumentException")
    public void uncompressUnsupportedThrows() {
        Bytes<?> input = Bytes.from("payload");
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            input.readPosition(0);
            input.readLimit(input.writePosition());
            assertThrows(IllegalArgumentException.class,
                    () -> Compression.uncompress("unsupported", input, output),
                    "Unsupported algorithm should throw an IllegalArgumentException");
        } finally {
            input.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("uncompress ignores non-binary B-prefixed algorithms")
    public void uncompressIgnoresNonBinaryPrefix() {
        Bytes<?> input = Bytes.from("payload");
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            input.readPosition(0);
            input.readLimit(input.writePosition());
            Compression.uncompress("bogus", input, output);
            assertEquals(0,
                    output.writePosition(),
                    "Non-binary B-prefixed algorithm should leave output unchanged");
        } finally {
            input.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @Disabled("Disabled until Compression.uncompress(CharSequence, T, ThrowingFunction) is removed")
    @DisplayName("byte array uncompress returns data or null for unknown algorithms")
    public void uncompressFunctionReturnsExpectedResult() {
        byte[] data = "inputData".getBytes(ISO_8859_1);
        byte[] binary = Compression.uncompress("binary", data, ignored -> data);
        byte[] lPrefix = Compression.uncompress("lzo", data, ignored -> data);
        byte[] unsupported = Compression.uncompress("unknown", data, ignored -> data);
        assertArrayEquals(data, binary,
                "Binary uncompress should return the source bytes");
        assertArrayEquals(new byte[0], lPrefix,
                "Unknown L-prefixed compression should return null");
        assertNull(unsupported,
                "Unknown compression should return null");
    }

    @Test
    @DisplayName("LZW compression round-trips through the static helpers")
    public void lzwCompressionRoundTrip() {
        byte[] original = "lzw payload content".getBytes(ISO_8859_1);
        Bytes<byte[]> uncompressed = Bytes.wrapForRead(original);
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(256);
        Bytes<?> output = Bytes.allocateElasticOnHeap(256);
        try {
            Compression.compress("lzw", uncompressed, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            Compression.uncompress("lzw", compressed, output);
            output.readPosition(0);
            output.readLimit(output.writePosition());
            assertArrayEquals(original,
                    output.toByteArray(),
                    "LZW compressed data should round-trip to the original bytes");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("GZIP compression round-trips through the static helpers")
    public void gzipCompressionRoundTrip() {
        byte[] original = "gzip payload content".getBytes(ISO_8859_1);
        Bytes<byte[]> uncompressed = Bytes.wrapForRead(original);
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(256);
        Bytes<?> output = Bytes.allocateElasticOnHeap(256);
        try {
            Compression.compress("gzip", uncompressed, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            Compression.uncompress("gzip", compressed, output);
            output.readPosition(0);
            output.readLimit(output.writePosition());
            assertArrayEquals(original,
                    output.toByteArray(),
                    "GZIP compressed data should round-trip to the original bytes");
        } finally {
            uncompressed.releaseLast();
            compressed.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("binary compression round-trips through default stream helpers")
    public void binaryCompressionRoundTripWithStreams() {
        Bytes<?> input = Bytes.from("stream payload");
        Bytes<?> compressed = Bytes.allocateElasticOnHeap(64);
        Bytes<?> output = Bytes.allocateElasticOnHeap(64);
        try {
            input.readPosition(0);
            input.readLimit(input.writePosition());
            Compressions.Binary.compress(input, compressed);
            compressed.readPosition(0);
            compressed.readLimit(compressed.writePosition());
            Compressions.Binary.uncompress(compressed, output);
            output.readPosition(0);
            output.readLimit(output.writePosition());
            assertEquals("stream payload",
                    new String(output.toByteArray(), ISO_8859_1),
                    "Binary stream compression should preserve the original payload");
        } finally {
            input.releaseLast();
            compressed.releaseLast();
            output.releaseLast();
        }
    }

    @Test
    @DisplayName("binary compression round-trips through byte array helpers")
    public void binaryCompressionRoundTripWithArrays() {
        byte[] original = "binary array payload".getBytes(ISO_8859_1);
        byte[] compressed = Compressions.Binary.compress(original);
        byte[] uncompressed = Compressions.Binary.uncompress(compressed);
        assertArrayEquals(original,
                uncompressed,
                "Binary byte array compression should preserve the original payload");
    }

    @Test
    @DisplayName("compress wraps stream failures in AssertionError")
    public void compressWrapsStreamFailures() {
        Compression failing = new FailingCompression();
        AssertionError error = assertThrows(AssertionError.class,
                () -> failing.compress("payload".getBytes(ISO_8859_1)),
                "Compression should wrap IO failures in an AssertionError");
        assertTrue(error.getMessage().contains("compress"),
                "Compression AssertionError message should mention compress");
    }

    @Test
    @DisplayName("uncompress wraps stream failures in IORuntimeException")
    public void uncompressWrapsStreamFailures() {
        Compression failing = new FailingCompression();
        IORuntimeException error = assertThrows(IORuntimeException.class,
                () -> failing.uncompress("payload".getBytes(ISO_8859_1)),
                "Uncompress should wrap IO failures in an IORuntimeException");
        assertTrue(error.getMessage().contains("uncompress"),
                "Uncompress exception message should mention uncompress");
    }

    private static final class FailingCompression implements Compression {
        @Override
        public InputStream decompressingStream(InputStream input) {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("read failure in test decompression stream");
                }
            };
        }

        @Override
        public OutputStream compressingStream(OutputStream output) {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    throw new IOException("write failure in test compression stream");
                }
            };
        }
    }
}
