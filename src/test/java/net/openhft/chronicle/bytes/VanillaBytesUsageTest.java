/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VanillaBytesUsageTest extends BytesTestCommon {

    @Test
    public void wrapNativeStoreMaintainsOffsets() {
        NativeBytesStore<Void> store = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        try {
            store.writeLong(0, 0x1122334455667788L);

            VanillaBytes<Void> bytes = VanillaBytes.wrap(store);
            try {
                bytes.readLimit(store.capacity());
                bytes.readPosition(0);
                assertEquals(0x1122334455667788L, bytes.readLong());
            } finally {
                bytes.releaseLast();
            }
        } finally {
            store.releaseLast();
        }
    }

    @Test
    public void vanillaBytesCanSwapUnderlyingStore() {
        NativeBytesStore<Void> storeA = NativeBytesStore.nativeStoreWithFixedCapacity(32);
        NativeBytesStore<Void> storeB = NativeBytesStore.nativeStoreWithFixedCapacity(32);
        try {
            storeA.writeUtf8(0, "alpha");
            storeB.writeUtf8(0, "beta");

            VanillaBytes<Void> reusable = VanillaBytes.vanillaBytes();
            try {
                reusable.bytesStore(storeA, 0, storeA.capacity());
                reusable.readPosition(0);
                assertEquals("alpha", reusable.readUtf8());

                reusable.bytesStore(storeB, 0, storeB.capacity());
                reusable.readPosition(0);
                assertEquals("beta", reusable.readUtf8());
            } finally {
                reusable.releaseLast();
            }
        } finally {
            storeA.releaseLast();
            storeB.releaseLast();
        }
    }

}
