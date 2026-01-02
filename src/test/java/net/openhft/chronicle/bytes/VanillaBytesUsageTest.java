/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VanillaBytesUsageTest extends BytesTestCommon {

    @Test
    @DisplayName("wrapping a native store preserves offsets and limits")
    public void wrapNativeStoreMaintainsOffsets() {
        NativeBytesStore<Void> store = NativeBytesStore.nativeStoreWithFixedCapacity(64);
        try {
            store.writeLong(0, 0x1122334455667788L);

            VanillaBytes<Void> bytes = VanillaBytes.wrap(store);
            try {
                bytes.readLimit(store.capacity());
                bytes.readPosition(0);
                assertEquals(0x1122334455667788L,
                        bytes.readLong(),
                        "Wrapped bytes should read the value written to the native store");
            } finally {
                bytes.releaseLast();
            }
        } finally {
            store.releaseLast();
        }
    }

    @Test
    @DisplayName("vanilla bytes can swap underlying stores safely")
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
                assertEquals("alpha",
                        reusable.readUtf8(),
                        "First store should return the expected UTF-8 value");

                reusable.bytesStore(storeB, 0, storeB.capacity());
                reusable.readPosition(0);
                assertEquals("beta",
                        reusable.readUtf8(),
                        "Second store should return the expected UTF-8 value");
            } finally {
                reusable.releaseLast();
            }
        } finally {
            storeA.releaseLast();
            storeB.releaseLast();
        }
    }

}
