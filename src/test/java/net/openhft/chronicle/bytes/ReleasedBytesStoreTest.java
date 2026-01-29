/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests released bytes store behaviour because accessing memory after release
 * can cause crashes or data corruption if resource lifecycle is mismanaged.
 */
@DisplayName("ReleasedBytesStore - validates post-release rejection of operations")
public class ReleasedBytesStoreTest extends BytesTestCommon {

    @Test
    @DisplayName("released bytes store rejects writes after release")
    public void release() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        assertNull(bytes.bytesStore().underlyingObject(),
                "Fresh bytes should not expose an underlying object yet");
        bytes.writeLong(0, 0);
        assertEquals(NativeBytesStore.class,
                bytes.bytesStore().getClass(),
                "Writing should initialise the native bytes store");
        bytes.releaseLast();
        assertEquals(0,
                bytes.bytesStore().refCount(),
                "Released bytes store should have a zero reference count");
        assertThrows(NullPointerException.class,
                () -> bytes.writeLong(0, 0),
                "Released bytes should reject further writes");
    }
}
