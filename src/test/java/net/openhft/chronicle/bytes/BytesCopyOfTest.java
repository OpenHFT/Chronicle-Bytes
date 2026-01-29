/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests BytesUtil.copyOf for preserving readable content because correct duplication
 * is required to avoid data loss when copying partial buffer views.
 */
@DisplayName("BytesUtil - copyOf preserves readable content from source position")
public class BytesCopyOfTest extends BytesTestCommon {

    @Test
    @DisplayName("copyOf returns direct bytes with same readable content")
    public void copyOfReturnsDirectBytesWithSameReadableContent() {
        Bytes<?> src = Bytes.allocateElasticOnHeap(32);
        try {
            src.append("lorem-ipsum");
            src.readSkip(6); // point to "ipsum"
            Bytes<Void> copy = BytesUtil.copyOf(src);
            try {
                assertEquals("ipsum", copy.toString(),
                        "copyOf preserves readable content");
                // copy is direct; avoid growing it to keep within fixed capacity
            } finally {
                copy.releaseLast();
            }
        } finally {
            src.releaseLast();
        }
    }
}
