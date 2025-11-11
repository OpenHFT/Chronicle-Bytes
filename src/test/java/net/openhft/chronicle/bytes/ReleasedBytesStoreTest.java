/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReleasedBytesStoreTest extends BytesTestCommon {

    @Test
    public void release() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        assertNull(bytes.bytesStore().underlyingObject());
        bytes.writeLong(0, 0);
        assertEquals(NativeBytesStore.class, bytes.bytesStore().getClass());
        bytes.releaseLast();
        assertEquals(0, bytes.bytesStore().refCount());
        try {
            bytes.writeLong(0, 0);
            fail();
        } catch (NullPointerException e) {
            // expected.
        }
    }
}
