/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import java.nio.BufferUnderflowException;

public class BytesInternalSubBytesErrorsTest extends BytesTestCommon {

    @Test(expected = BufferUnderflowException.class)
    public void subBytesThrowsWhenLengthTooLarge() {
        Bytes<?> src = Bytes.from("abc");
        try {
            // request a sub view longer than remaining
            BytesInternal.subBytes(src, 0, 10);
        } finally {
            src.releaseLast();
        }
    }
}

