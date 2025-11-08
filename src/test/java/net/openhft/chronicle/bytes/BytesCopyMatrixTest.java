/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BytesCopyMatrixTest extends BytesTestCommon {

    @Test
    public void heapToNativeStoreCopiesReadablePortion() {
        Bytes<?> source = Bytes.allocateElasticOnHeap();
        source.append("alpha-beta");
        source.readPosition(6); // start at beta
        BytesStore<?, Void> target = BytesStore.nativeStoreWithFixedCapacity((int) source.readRemaining());
        try {
            long copied = source.copyTo(target);
            assertEquals(source.readRemaining(), copied);
            assertEquals(6, source.readPosition());

            Bytes<?> view = target.bytesForRead();
            try {
                byte[] data = new byte[(int) copied];
                view.read(data);
                assertArrayEquals("beta".getBytes(StandardCharsets.ISO_8859_1), data);
            } finally {
                view.releaseLast();
            }
        } finally {
            target.releaseLast();
            source.releaseLast();
        }
    }

    @Test
    public void directBytesCopyToOutputStream() throws IOException {
        Bytes<?> source = Bytes.allocateDirect(32);
        try {
            source.append("payload");
            source.readPosition(0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            source.copyTo(baos);
            assertEquals("payload", baos.toString(StandardCharsets.ISO_8859_1.name()));
            assertEquals("copyTo(OutputStream) must not move readPosition", 0, source.readPosition());
        } finally {
            source.releaseLast();
        }
    }
}
