/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertThrows;

public class RandomDataInputUtf8LimitedMoreTest extends BytesTestCommon {

    @Test
    public void bufferUnderflowWhenDeclaredLengthExceedsRemaining() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(16);
        try {
            // Write stop-bit length larger than the following data
            b.writeStopBit(10);
            b.append("abc");
            StringBuilder sb = new StringBuilder();
            assertThrows(BufferUnderflowException.class,
                    () -> b.readUtf8Limited(0, sb, 20));
        } finally {
            b.releaseLast();
        }
    }
}

