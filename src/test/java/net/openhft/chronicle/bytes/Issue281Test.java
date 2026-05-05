/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Issue 281 byte buffer to bytes conversion")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class Issue281Test extends BytesTestCommon {
    private static void bufferToBytes(Bytes<?> bytes, ByteBuffer dataBuffer, int index) {
        int length = dataBuffer.get(index); // length prefix (offset)
        bytes.write(0, dataBuffer, index + 1, length);
        bytes.writeSkip(length);
    }

    @Test
    @DisplayName("byte buffer copy respects byte order")
    public void testByteBufferToBytes() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for byte buffer test");

        final Bytes<?> data = Bytes.allocateElasticDirect().append("1234567890ABCD");
        final Bytes<?> retVal = Bytes.allocateElasticDirect();
        ByteBuffer buffer = ByteBuffer.allocateDirect(16);
        String test = "1234567890ABCD";
        buffer.put((byte) test.length());
        for (byte b : test.getBytes(StandardCharsets.UTF_8)) {
            buffer.put(b);
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        bufferToBytes(retVal, buffer, 0); // this calls bufferToBytes below
        assertTrue(data.contentEquals(retVal),
                "Little endian copy preserves content equality");

        retVal.clear();
        buffer.order(ByteOrder.BIG_ENDIAN);
        bufferToBytes(retVal, buffer, 0); // this calls bufferToBytes below
        assertTrue(data.contentEquals(retVal),
                "Big endian copy preserves content equality");
    }
}
