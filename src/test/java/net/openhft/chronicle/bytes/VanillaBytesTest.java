/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VanillaBytesTest extends BytesTestCommon {

    @Test
    void testBytesForRead() {
        byte[] byteArr = new byte[128];
        for (int i = 0; i < byteArr.length; i++)
            byteArr[i] = (byte) i;
        Bytes<?> bytes = Bytes.wrapForRead(byteArr);
        bytes.readSkip(8);
        @NotNull Bytes<?> bytes2 = bytes.bytesForRead();
        assertEquals(128 - 8, bytes2.readRemaining(), "bytes2.readRemaining");
        assertEquals(8, bytes2.readPosition(), "bytesForRead should preserve read position at 8 after readSkip");
        assertEquals(8, bytes2.readByte(bytes2.start()), "byte at start position should be 8 from original array");
        assertEquals(9, bytes2.readByte(bytes2.start() + 1), "byte at start+1 position should be 9 from original array");
        assertEquals(9, bytes.readByte(9), "byte at position 9 should be 9 from original array");
        bytes2.writeByte(bytes2.start() + 1, 99);
        assertEquals(99, bytes.readByte(99), "byte at position 99 should be 99 after writeByte through bytesForRead");

        bytes.releaseLast();
    }
}
