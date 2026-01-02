/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Consolidated IO and copy behaviour tests for BytesInternal data transfers,
 * buffer slices, and stream round trip operations.
 */
@DisplayName("BytesInternal IO copy operations and byte array transfers")
public class BytesInternalIOCopyTest extends BytesTestCommon {

    @Test
    @DisplayName("copy bytes to output stream and byte array")
    public void copyFromRandomDataInputToOutputStreamAndToByteArray() throws IOException {
        final Bytes<?> src = Bytes.from("abcdef");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            BytesInternal.copy(src, bos);
            assertArrayEquals("abcdef".getBytes(StandardCharsets.ISO_8859_1), bos.toByteArray(),
                    "Output stream copy preserves bytes");

            byte[] arr = BytesInternal.toByteArray(src);
            assertArrayEquals("abcdef".getBytes(StandardCharsets.ISO_8859_1), arr,
                    "toByteArray copies source bytes");

            // subBytes view from heap-backed input
            BytesStore<?, ?> sub = BytesInternal.subBytes(src, 2, 3);
            byte[] got = new byte[3];
            long n = sub.read(0, got, 0, 3);
            assertEquals(3L, n,
                    "Sub-bytes read returns expected length");
            assertArrayEquals("cde".getBytes(StandardCharsets.ISO_8859_1), got,
                    "Sub-bytes view returns expected slice");
        } finally {
            src.releaseLast();
        }
    }

    @Test
    @DisplayName("copy input stream and direct bytes to arrays")
    public void copyInputStreamLargeAndDirectToArray() throws Exception {
        byte[] data = new byte[2000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 0x7F);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        Bytes<?> out = Bytes.allocateElasticOnHeap(128);
        try {
            BytesInternal.copy(bis, out);
            assertEquals(data.length, out.length(),
                    "Input stream copy preserves length");

            // direct memory variant of toByteArray
            Bytes<?> direct = Bytes.allocateDirect(6);
            try {
                direct.append("123456");
                direct.readPosition(0);
                byte[] arr = BytesInternal.toByteArray(direct);
                assertArrayEquals("123456".getBytes(StandardCharsets.ISO_8859_1), arr,
                        "Direct bytes toByteArray returns full data");
            } finally {
                direct.releaseLast();
            }
        } finally {
            out.releaseLast();
        }
    }
}
