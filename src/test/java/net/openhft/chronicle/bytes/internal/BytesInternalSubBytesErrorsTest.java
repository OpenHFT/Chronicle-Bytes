/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BytesInternalSubBytesErrorsTest extends BytesTestCommon {

    @Test
    public void subBytesThrowsWhenLengthTooLarge() {
        assertThrows(BufferUnderflowException.class, () -> {
            Bytes<?> src = Bytes.from("abc");
            try {
                // request a sub view longer than remaining
                BytesInternal.subBytes(src, 0, 10);
            } finally {
                src.releaseLast();
            }
        });
    }
}

