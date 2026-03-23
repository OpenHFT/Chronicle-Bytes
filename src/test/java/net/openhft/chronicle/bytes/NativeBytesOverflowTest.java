/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.BytesStore.wrap;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class NativeBytesOverflowTest extends BytesTestCommon {

    @Test
    public void testExceedWriteLimitNativeWriteBytes() {
        assertThrows(BufferOverflowException.class, () -> {
            BytesStore<?, ByteBuffer> store = wrap(ByteBuffer.allocate(128));
            Bytes<?> nb = new NativeBytes<>(store);
            try {
                nb.writeLimit(2).writePosition(0);
                nb.writeLong(10L);
            } finally {
                nb.releaseLast();
            }
        });
    }

    @Test
    public void testExceedWriteLimitGuardedBytes() {
        assertThrows(BufferOverflowException.class, () -> {
            Bytes<?> guardedNativeBytes = new GuardedNativeBytes<>(wrap(ByteBuffer.allocate(128)), 128);
            try {
                guardedNativeBytes.writeLimit(2).writePosition(0);
                guardedNativeBytes.writeLong(10L);
            } finally {
                guardedNativeBytes.releaseLast();
            }
        });
    }

    @Test
    public void testElastic() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        assertThrows(BufferOverflowException.class, () -> {
            Bytes<?> bytes = Bytes.elasticByteBuffer();
            try {
                bytes.writeLimit(2).writePosition(0);
                bytes.writeLong(10L);
            } finally {
                bytes.releaseLast();
            }
        });
    }

    @Test
    public void testNativeWriteBytes2() {
        Bytes<?> nb = new NativeBytes<>(wrap(ByteBuffer.allocate(128))).unchecked(true);

        nb.writeLimit(2).writePosition(0);
        nb.writeLong(10L);

        // this is OK as we are unchecked !
        assertTrue(nb.writePosition() > nb.writeLimit());
    }
}
