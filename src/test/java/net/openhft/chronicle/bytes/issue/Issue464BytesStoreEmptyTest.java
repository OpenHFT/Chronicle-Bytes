/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class Issue464BytesStoreEmptyTest extends BytesTestCommon {
    @Test
    public void emptyShouldNotAllocate() {
        assertSame(BytesStore.empty(), BytesStore.empty(), "emptyShouldNotAllocate: same instance");
    }

    @Test
    public void nullByteArrayShouldNotAllocate() {
        assertThrows(NullPointerException.class, () -> BytesStore.wrap((byte[]) null));
    }

    @Test
    public void allocateEmptyStringShouldNotAllocate() {
        assertSame(BytesStore.from(""), BytesStore.from(""), "allocateEmptyStringShouldNotAllocate: same instance");
    }

    @Test
    public void emptyBytesStoreShouldNotAllocate() {
        assertSame(BytesStore.from(BytesStore.empty()), BytesStore.from(BytesStore.empty()), "emptyBytesStoreShouldNotAllocate: same instance");
    }

    @Test
    public void emptyStringBuilderShouldNotAllocate() {
        assertSame(BytesStore.from(new StringBuilder()), BytesStore.from(new StringBuilder()), "emptyStringBuilderShouldNotAllocate: same instance");
    }

    @Test
    public void nullNativeStoreFromShouldNotAllocate() {
        assertThrows(NullPointerException.class, () ->
                BytesStore.nativeStoreFrom(null));
    }

    @Test
    public void emptyCopyFromShouldNotAllocate() {
        assertSame(BytesStore.empty().copy(), BytesStore.empty().copy(), "emptyCopyFromShouldNotAllocate: same instance");
    }

    @Test
    public void emptyByteArrayShouldHaveDifferentUnderlying() {
        BytesStore<?, byte[]> a = BytesStore.wrap(new byte[0]);
        BytesStore<?, byte[]> b = BytesStore.wrap(new byte[0]);
        assertNotSame(a, b, "wrapping two empty byte arrays should create different BytesStore instances");
        assertNotSame(a.underlyingObject(), b.underlyingObject(), "a.underlyingObject");
    }

}
