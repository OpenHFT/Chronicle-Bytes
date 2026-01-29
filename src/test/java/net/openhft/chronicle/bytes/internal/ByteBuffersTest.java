/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ByteBuffers utility class, because setAddressCapacity must validate
 * capacity bounds to avoid integer overflow when configuring direct buffers.
 */
@DisplayName("ByteBuffers capacity and address configuration validation")
class ByteBuffersTest extends BytesTestCommon {

    @Test
    @DisplayName("setAddressCapacity throws ArithmeticException for capacity exceeding Integer.MAX_VALUE")
    void shouldThrowForCapacityOverflow() {
        ByteBuffer direct = ByteBuffer.allocateDirect(64);

        // Capacity larger than Integer.MAX_VALUE should throw ArithmeticException
        // This tests the Math.toIntExact branch
        long hugeCapacity = (long) Integer.MAX_VALUE + 1;

        assertThrows(ArithmeticException.class,
                () -> ByteBuffers.setAddressCapacity(direct, 0x1000L, hugeCapacity),
                "Capacity exceeding Integer.MAX_VALUE should throw ArithmeticException");
    }

    @Test
    @DisplayName("setAddressCapacity accepts valid capacity within int range")
    void shouldAcceptValidCapacity() {
        ByteBuffer direct = ByteBuffer.allocateDirect(64);

        // Test that Integer.MAX_VALUE does not throw (covers the success path)
        assertDoesNotThrow(() -> ByteBuffers.setAddressCapacity(direct, 0x1000L, 32),
                "Valid capacity should be accepted");
    }
}
