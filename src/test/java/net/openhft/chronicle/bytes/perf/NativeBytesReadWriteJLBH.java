/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.perf;

import net.openhft.chronicle.bytes.Bytes;

/**
 * Convenience entry point for the Bytes read/write JLBH benchmark because
 * measuring native buffer throughput is essential to validate off-heap
 * performance characteristics.
 */
public class NativeBytesReadWriteJLBH {

    public static void main(String[] args) {
        BytesReadWriteJLBH.runForBytes(Bytes.allocateElasticDirect(1024 * 1024));
    }
}
