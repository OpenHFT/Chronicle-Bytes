/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BytesCopyMatrixTest extends BytesTestCommon {

    @Test
    public void heapToNativeStoreCopiesReadablePortion() {
        Bytes<?> source = Bytes.allocateElasticOnHeap();
        source.append("alpha-beta");
        source.readPosition(6); // start at beta
        BytesStore<?, Void> target = BytesStore.nativeStoreWithFixedCapacity((int) source.readRemaining());
        try {
            long copied = source.copyTo(target);
            assertEquals(source.readRemaining(), copied, "copyTo should return number of bytes copied (equals readRemaining)");
            assertEquals(6, source.readPosition(), "copyTo should not advance source read position");

            Bytes<?> view = target.bytesForRead();
            try {
                byte[] data = new byte[(int) copied];
                view.read(data);
                assertArrayEquals("beta".getBytes(ISO_8859_1), data, "heapToNativeStoreCopiesReadablePortion: assertArrayEquals");
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
            assertEquals("payload", baos.toString(ISO_8859_1.name()), "baos.toString");
            assertEquals(0, source.readPosition(), "copyTo(OutputStream) must not move readPosition");
        } finally {
            source.releaseLast();
        }
    }
}
