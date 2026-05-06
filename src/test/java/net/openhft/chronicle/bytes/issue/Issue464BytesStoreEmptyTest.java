/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.*;

public class Issue464BytesStoreEmptyTest extends BytesTestCommon {
    @Test
    public void emptyShouldNotAllocate() {
        doTest(BytesStore::empty);
    }

    @Test
    public void nullByteArrayShouldNotAllocate() {
        assertThrows(NullPointerException.class, () -> BytesStore.wrap((byte[]) null));
    }

    @Test
    public void allocateEmptyStringShouldNotAllocate() {
        doTest(() -> BytesStore.from(""));
    }

    @Test
    public void emptyBytesStoreShouldNotAllocate() {
        doTest(() -> BytesStore.from(BytesStore.empty()));
    }

    @Test
    public void emptyStringBuilderShouldNotAllocate() {
        doTest(() -> BytesStore.from(new StringBuilder()));
    }

    @Test(expected = NullPointerException.class)
    public void nullNativeStoreFromShouldNotAllocate() {
        doTest(() -> BytesStore.nativeStoreFrom(null));
    }

    @Test
    public void emptyCopyFromShouldNotAllocate() {
        doTest(() -> BytesStore.empty().copy());
    }

    @Test
    public void emptyByteArrayShouldHaveDifferentUnderlying() {
        BytesStore<?, byte[]> a = BytesStore.wrap(new byte[0]);
        BytesStore<?, byte[]> b = BytesStore.wrap(new byte[0]);
        assertNotSame(a, b);
        assertNotSame(a.underlyingObject(), b.underlyingObject());
    }

    private void doTest(Supplier<BytesStore<?, ?>> supplier) {
        assertSame(supplier.get(), supplier.get());
    }
}
