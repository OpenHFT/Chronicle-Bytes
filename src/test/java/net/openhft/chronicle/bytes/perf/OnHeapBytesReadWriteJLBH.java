/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.perf;

import net.openhft.chronicle.bytes.Bytes;

public class OnHeapBytesReadWriteJLBH {

    public static void main(String[] args) {
        BytesReadWriteJLBH.runForBytes(Bytes.allocateElasticOnHeap(1024 * 1024));
    }
}
