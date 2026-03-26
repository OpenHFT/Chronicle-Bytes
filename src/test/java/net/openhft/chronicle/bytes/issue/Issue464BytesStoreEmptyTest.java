/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class Issue464BytesStoreEmptyTest extends BytesTestCommon {
    @Test
    void emptyShouldNotAllocate() {
        doTest(BytesStore::empty);
    }

    @Test
    void nullByteArrayShouldNotAllocate() {
        assertThrows(NullPointerException.class, () -> BytesStore.wrap((byte[]) null));
    }

    @Test
    void allocateEmptyStringShouldNotAllocate() {
        doTest(() -> BytesStore.from(""));
    }

    @Test
    void emptyBytesStoreShouldNotAllocate() {
        doTest(() -> BytesStore.from(BytesStore.empty()));
    }

    @Test
    void emptyStringBuilderShouldNotAllocate() {
        doTest(() -> BytesStore.from(new StringBuilder()));
    }

    @Test
    void nullNativeStoreFromShouldNotAllocate() {
        assertThrows(NullPointerException.class, () -> doTest(() -> BytesStore.nativeStoreFrom(null)));
    }

    @Test
    void emptyCopyFromShouldNotAllocate() {
        doTest(() -> BytesStore.empty().copy());
    }

    @Test
    void emptyByteArrayShouldHaveDifferentUnderlying() {
        BytesStore<?, byte[]> a = BytesStore.wrap(new byte[0]);
        BytesStore<?, byte[]> b = BytesStore.wrap(new byte[0]);
        assertNotSame(a, b);
        assertNotSame(a.underlyingObject(), b.underlyingObject());
    }

    private void doTest(Supplier<BytesStore<?, ?>> supplier) {
        assertSame(supplier.get(), supplier.get());
    }
}
