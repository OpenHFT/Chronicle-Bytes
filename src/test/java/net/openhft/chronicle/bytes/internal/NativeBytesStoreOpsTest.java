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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NativeBytesStoreOpsTest extends BytesTestCommon {

    @Test
    public void readWriteAndVolatileOrderedOps() {
        BytesStore<?, ?> store = BytesStore.nativeStore(32);
        try {
            long off = 0;
            store.writeLong(off, 0x0102030405060708L);
            assertEquals(0x0102030405060708L, store.readLong(off));
            store.writeInt(off + 8, 0x11223344);
            assertEquals(0x11223344, store.readInt(off + 8));

            store.writeVolatileLong(off, 9L);
            assertEquals(9L, store.readVolatileLong(off));
            store.writeOrderedLong(off, 10L);
            assertEquals(10L, store.readLong(off));
            assertEquals(15L, store.addAndGetLong(off, 5L));
        } finally {
            store.releaseLast();
        }
    }
}

