/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

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
            assertArrayEquals("abcdef".getBytes(), bos.toByteArray());

            byte[] arr = BytesInternal.toByteArray(src);
            assertArrayEquals("abcdef".getBytes(), arr);

            // subBytes view from heap-backed input
            BytesStore<?, ?> sub = BytesInternal.subBytes(src, 2, 3);
            byte[] got = new byte[3];
            long n = sub.read(0, got, 0, 3);
            assertEquals(3L, n);
            assertArrayEquals("cde".getBytes(), got);
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
            assertEquals(data.length, out.length());

            // direct memory variant of toByteArray
            Bytes<?> direct = Bytes.allocateDirect(6);
            try {
                direct.append("123456");
                direct.readPosition(0);
                byte[] arr = BytesInternal.toByteArray(direct);
                assertArrayEquals("123456".getBytes(), arr);
            } finally {
                direct.releaseLast();
            }
        } finally {
            out.releaseLast();
        }
    }
}

