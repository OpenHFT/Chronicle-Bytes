/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VanillaBytesUsageTest extends BytesTestCommon {

    @Test
    public void wrapNativeStoreMaintainsOffsets() throws Exception {
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
    public void vanillaBytesCanSwapUnderlyingStore() throws Exception {
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
