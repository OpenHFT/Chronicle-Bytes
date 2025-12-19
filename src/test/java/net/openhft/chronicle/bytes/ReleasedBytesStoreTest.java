/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReleasedBytesStoreTest extends BytesTestCommon {

    @Test
    public void release() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        assertNull(bytes.bytesStore().underlyingObject(), "Elastic bytes should start with null underlying object before first write");
        bytes.writeLong(0, 0);
        assertEquals(NativeBytesStore.class, bytes.bytesStore().getClass(), "First write should allocate NativeBytesStore");
        bytes.releaseLast();
        assertEquals(0, bytes.bytesStore().refCount(), "BytesStore ref count should be zero after release");
        assertThrows(NullPointerException.class, () -> bytes.writeLong(0, 0));
    }
}
