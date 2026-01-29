/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests UTF-8 limited read behaviour because correct underflow detection
 * is essential to avoid reading beyond declared string boundaries in
 * binary protocols.
 */
@DisplayName("UTF8 limited reads detect underflow when declared length exceeds remaining")
public class RandomDataInputUtf8LimitedMoreTest extends BytesTestCommon {

    @Test
    @DisplayName("buffer underflow when length exceeds remaining bytes")
    public void bufferUnderflowWhenDeclaredLengthExceedsRemaining() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(16);
        try {
            // Write stop-bit length larger than the following data
            b.writeStopBit(10);
            b.append("abc");
            StringBuilder sb = new StringBuilder();
            assertThrows(BufferUnderflowException.class,
                    () -> b.readUtf8Limited(0, sb, 20),
                    "UTF8 limited read fails when declared length exceeds data");
        } finally {
            b.releaseLast();
        }
    }
}
