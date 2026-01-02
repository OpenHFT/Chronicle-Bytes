/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VanillaBytesTest extends BytesTestCommon {

    @Test
    @DisplayName("bytesForRead shares position and content state between views")
    void testBytesForRead() {
        byte[] byteArr = new byte[128];
        for (int i = 0; i < byteArr.length; i++)
            byteArr[i] = (byte) i;
        Bytes<?> bytes = Bytes.wrapForRead(byteArr);
        bytes.readSkip(8);
        @NotNull Bytes<?> bytes2 = bytes.bytesForRead();
        assertEquals(128 - 8,
                bytes2.readRemaining(),
                "Read view should expose remaining bytes from the shared position");
        assertEquals(8,
                bytes2.readPosition(),
                "Read view should start at the shared read position");
        assertEquals(8,
                bytes2.readByte(bytes2.start()),
                "First byte in the view should reflect the skipped offset");
        assertEquals(9,
                bytes2.readByte(bytes2.start() + 1),
                "Second byte in the view should follow the skipped offset");
        assertEquals(9,
                bytes.readByte(9),
                "Original bytes should still expose the expected data at index 9");
        bytes2.writeByte(bytes2.start() + 1, 99);
        assertEquals(99,
                bytes.readByte(99),
                "Writing through the view should be visible in the original bytes");

        bytes.releaseLast();
    }
}
