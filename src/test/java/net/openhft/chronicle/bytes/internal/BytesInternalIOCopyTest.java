/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Consolidated IO and copy tests for BytesInternal.
 */
public class BytesInternalIOCopyTest extends BytesTestCommon {

    @Test
    public void copyFromRandomDataInputToOutputStreamAndToByteArray() throws IOException {
        final Bytes<?> src = Bytes.from("abcdef");
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            BytesInternal.copy(src, bos);
            assertArrayEquals("abcdef".getBytes(ISO_8859_1), bos.toByteArray(), "copyFromRandomDataInputToOutputStreamAndToByteArray: assertArrayEquals");

            byte[] arr = BytesInternal.toByteArray(src);
            assertArrayEquals("abcdef".getBytes(ISO_8859_1), arr, "copyFromRandomDataInputToOutputStreamAndToByteArray: assertArrayEquals");

            // subBytes view from heap-backed input
            BytesStore<?, ?> sub = BytesInternal.subBytes(src, 2, 3);
            byte[] got = new byte[3];
            long n = sub.read(0, got, 0, 3);
            assertEquals(3L, n, "subBytes.read should return number of bytes read (3)");
            assertArrayEquals("cde".getBytes(ISO_8859_1), got, "copyFromRandomDataInputToOutputStreamAndToByteArray: assertArrayEquals");
        } finally {
            src.releaseLast();
        }
    }

    @Test
    public void copyInputStreamLargeAndDirectToArray() throws Exception {
        byte[] data = new byte[2000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 0x7F);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        Bytes<?> out = Bytes.allocateElasticOnHeap(128);
        try {
            BytesInternal.copy(bis, out);
            assertEquals(data.length, out.length(), "copied Bytes length should equal source InputStream data length (2000)");

            // direct memory variant of toByteArray
            Bytes<?> direct = Bytes.allocateDirect(6);
            try {
                direct.append("123456");
                direct.readPosition(0);
                byte[] arr = BytesInternal.toByteArray(direct);
                assertArrayEquals("123456".getBytes(ISO_8859_1), arr, "copyInputStreamLargeAndDirectToArray: assertArrayEquals");
            } finally {
                direct.releaseLast();
            }
        } finally {
            out.releaseLast();
        }
    }
}
