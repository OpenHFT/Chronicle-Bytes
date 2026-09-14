/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.algo;

import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertThrows;

public class OptimisedHashRangeTest {
    private static BytesStore<?, ?> nonContiguousStore() {
        return (BytesStore<?, ?>) Proxy.newProxyInstance(BytesStore.class.getClassLoader(),
                new Class<?>[]{BytesStore.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "bytesStore": return proxy;
                        case "readPosition": return 4092L;
                        case "inside": return false;
                        case "addressForRead": throw new AssertionError("address requested before range rejection");
                        default: throw new AssertionError("unexpected call: " + method.getName());
                    }
                });
    }

    @Test
    public void dispatcherRejectsBeforeAddressAcquisition() {
        assertThrows(UnsupportedOperationException.class,
                () -> OptimisedBytesStoreHash.INSTANCE.applyAsLong(nonContiguousStore(), 32));
    }

    @Test
    public void multipleOf32RejectsBeforeAddressAcquisition() {
        assertThrows(UnsupportedOperationException.class,
                () -> OptimisedBytesStoreHash.applyAsLong32bytesMultiple(nonContiguousStore(), 32));
    }

    @Test
    public void arbitraryLengthRejectsBeforeAddressAcquisition() {
        assertThrows(UnsupportedOperationException.class,
                () -> OptimisedBytesStoreHash.applyAsLongAny(nonContiguousStore(), 33));
    }
}
