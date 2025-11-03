/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue462Test {

    private static Stream<Bytes<ByteBuffer>> bytesToTest() {
        if (Jvm.maxDirectMemory() == 0) {
            return Stream.of(
                    Bytes.elasticHeapByteBuffer(),
                    Bytes.elasticHeapByteBuffer(128));
        }
        return Stream.of(
                Bytes.elasticByteBuffer(),
                Bytes.elasticByteBuffer(128),
                Bytes.elasticByteBuffer(128, 256),
                Bytes.elasticHeapByteBuffer(),
                Bytes.elasticHeapByteBuffer(128));
    }

    @ParameterizedTest
    @MethodSource("bytesToTest")
    void testByteBufferByteOrder(Bytes<ByteBuffer> bytes) {
        final long value = 0x0102030405060708L;
        bytes.writeLong(value);
        final ByteBuffer byteBuffer = bytes.underlyingObject();
        assertEquals(ByteOrder.nativeOrder(), byteBuffer.order());
        final long aLong = byteBuffer.getLong();
        assertEquals(Long.toHexString(value), Long.toHexString(aLong));
        assertEquals(value, aLong);
        byteBuffer.putDouble(0, 0.1);
        final double actual = bytes.readDouble();
        assertEquals(Long.toHexString(Double.doubleToLongBits(0.1)), Long.toHexString(Double.doubleToLongBits(actual)));
        assertEquals(0.1, actual, 0.0);
    }
}
