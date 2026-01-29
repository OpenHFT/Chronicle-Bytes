/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BytesInternal subBytes error handling for boundary conditions")
public class BytesInternalSubBytesErrorsTest extends BytesTestCommon {

    @Test
    @DisplayName("subBytes throws when requested length exceeds remaining data")
    public void subBytesThrowsWhenLengthTooLarge() {
        assertThrows(BufferUnderflowException.class, () -> {
            Bytes<?> src = Bytes.from("abc");
            try {
                // request a sub view longer than remaining
                BytesInternal.subBytes(src, 0, 10);
            } finally {
                src.releaseLast();
            }
        }, "subBytes should throw underflow when length 10 exceeds source length 3");
    }
}
