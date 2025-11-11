/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesCopyOfTest extends BytesTestCommon {

    @Test
    public void copyOfReturnsDirectBytesWithSameReadableContent() {
        Bytes<?> src = Bytes.allocateElasticOnHeap(32);
        try {
            src.append("lorem-ipsum");
            src.readSkip(6); // point to "ipsum"
            Bytes<Void> copy = BytesUtil.copyOf(src);
            try {
                assertEquals("ipsum", copy.toString());
                // copy is direct; avoid growing it to keep within fixed capacity
            } finally {
                copy.releaseLast();
            }
        } finally {
            src.releaseLast();
        }
    }
}
