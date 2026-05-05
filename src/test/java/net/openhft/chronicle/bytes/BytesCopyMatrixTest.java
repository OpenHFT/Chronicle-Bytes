/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests copyTo across heap and native byte stores because correct data transfer is
 * required to avoid corruption when moving bytes between different memory regions.
 */
@DisplayName("Bytes - copyTo matrix tests for heap and native store combinations")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class BytesCopyMatrixTest extends BytesTestCommon {

    @Test
    @DisplayName("copy heap bytes to native store preserves readable data")
    public void heapToNativeStoreCopiesReadablePortion() {
        Bytes<?> source = Bytes.allocateElasticOnHeap();
        source.append("alpha-beta");
        source.readPosition(6); // start at beta
        BytesStore<?, Void> target = BytesStore.nativeStoreWithFixedCapacity((int) source.readRemaining());
        try {
            long copied = source.copyTo(target);
            assertEquals(source.readRemaining(), copied,
                    "copyTo returns remaining length for heap source");
            assertEquals(6, source.readPosition(),
                    "read position remains after copy for heap source");

            Bytes<?> view = target.bytesForRead();
            try {
                byte[] data = new byte[(int) copied];
                view.read(data);
                assertArrayEquals("beta".getBytes(StandardCharsets.ISO_8859_1), data,
                        "copied bytes match expected payload");
            } finally {
                view.releaseLast();
            }
        } finally {
            target.releaseLast();
            source.releaseLast();
        }
    }

    @Test
    @DisplayName("copy direct bytes to output stream preserves read position")
    public void directBytesCopyToOutputStream() throws IOException {
        Bytes<?> source = Bytes.allocateDirect(32);
        try {
            source.append("payload");
            source.readPosition(0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            source.copyTo(baos);
            assertEquals("payload", baos.toString(StandardCharsets.ISO_8859_1.name()),
                    "copyTo(OutputStream) writes expected payload");
            assertEquals(0, source.readPosition(),
                    "copyTo(OutputStream) must not move readPosition");
        } finally {
            source.releaseLast();
        }
    }
}
