/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Empty BytesStore allocation avoids new backing")
public class Issue464BytesStoreEmptyTest extends BytesTestCommon {
    @Test
    @DisplayName("empty bytes store uses shared empty instance")
    public void emptyShouldNotAllocate() {
        doTest(BytesStore::empty, "BytesStore.empty");
    }

    @Test
    @DisplayName("null byte array wrap refuses allocation")
    public void nullByteArrayShouldNotAllocate() {
        assertThrows(NullPointerException.class,
                () -> BytesStore.wrap((byte[]) null),
                "Wrap rejects null byte array input");
    }

    @Test
    @DisplayName("empty string store uses shared instance")
    public void allocateEmptyStringShouldNotAllocate() {
        doTest(() -> BytesStore.from(""), "BytesStore.from empty string");
    }

    @Test
    @DisplayName("empty bytes store from store uses shared instance")
    public void emptyBytesStoreShouldNotAllocate() {
        doTest(() -> BytesStore.from(BytesStore.empty()), "BytesStore.from empty store");
    }

    @Test
    @DisplayName("empty string builder store uses shared instance")
    public void emptyStringBuilderShouldNotAllocate() {
        doTest(() -> BytesStore.from(new StringBuilder()), "BytesStore.from empty StringBuilder");
    }

    @Test
    @DisplayName("null native store from rejects allocation")
    public void nullNativeStoreFromShouldNotAllocate() {
        assertThrows(NullPointerException.class,
                () -> BytesStore.nativeStoreFrom(null),
                "Native store from rejects null input");
    }

    @Test
    @DisplayName("empty bytes store copy uses shared instance")
    public void emptyCopyFromShouldNotAllocate() {
        doTest(() -> BytesStore.empty().copy(), "BytesStore.empty copy");
    }

    @Test
    @DisplayName("empty byte arrays create distinct backing objects")
    public void emptyByteArrayShouldHaveDifferentUnderlying() {
        BytesStore<?, byte[]> a = BytesStore.wrap(new byte[0]);
        BytesStore<?, byte[]> b = BytesStore.wrap(new byte[0]);
        assertNotSame(a, b,
                "Distinct wraps return distinct bytes store instances");
        assertNotSame(a.underlyingObject(), b.underlyingObject(),
                "Underlying byte arrays are distinct for empty wraps");
    }

    private void doTest(Supplier<BytesStore<?, ?>> supplier, String scenario) {
        assertSame(supplier.get(), supplier.get(),
                "Repeated supplier call returns same instance for " + scenario);
    }
}
