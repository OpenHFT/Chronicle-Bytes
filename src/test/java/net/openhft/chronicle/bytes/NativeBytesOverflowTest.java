/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Validates that {@link NativeBytes} enforces write limits and reports buffer
 * overflows with enriched exceptions.
 */
@DisplayName("Native bytes overflow exceptions for write limits")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class NativeBytesOverflowTest extends BytesTestCommon {

    @Test
    @DisplayName("native bytes write exceeds small limit")
    public void testExceedWriteLimitNativeWriteBytes() {
        BytesStore<?, ByteBuffer> store = wrap(ByteBuffer.allocate(128));
        Bytes<?> nb = new NativeBytes<>(store);
        try {
            nb.writeLimit(2).writePosition(0);
            assertThrows(BufferOverflowException.class,
                    () -> nb.writeLong(10L),
                    "Native bytes write should overflow when limit is two");
        } finally {
            nb.releaseLast();
        }
    }

    @Test
    @DisplayName("guarded bytes write exceeds small limit")
    public void testExceedWriteLimitGuardedBytes() {
        Bytes<?> guardedNativeBytes = new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(128)), 128);
        try {
            guardedNativeBytes.writeLimit(2).writePosition(0);
            assertThrows(BufferOverflowException.class,
                    () -> guardedNativeBytes.writeLong(10L),
                    "Guarded bytes write should overflow when limit is two");
        } finally {
            guardedNativeBytes.releaseLast();
        }
    }

    @Test
    @DisplayName("elastic byte buffer overflows at small limit")
    public void testElastic() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for elastic overflow test");

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        try {
            bytes.writeLimit(2).writePosition(0);
            assertThrows(BufferOverflowException.class,
                    () -> bytes.writeLong(10L),
                    "Elastic bytes write should overflow when limit is two");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("unchecked native bytes allow position beyond limit")
    public void testNativeWriteBytes2() {
        Bytes<?> nb = new NativeBytes<>(wrap(ByteBuffer.allocate(128))).unchecked(true);

        nb.writeLimit(2).writePosition(0);
        nb.writeLong(10L);

        // this is OK as we are unchecked !
        assertTrue(nb.writePosition() > nb.writeLimit(),
                "Unchecked write position exceeds enforced limit");
    }
}
