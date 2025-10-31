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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HeapBytesStoreOpsTest extends BytesTestCommon {

    @Test
    public void heapStorePrimitiveOps() {
        Bytes<?> heap = Bytes.allocateElasticOnHeap(32);
        try {
            BytesStore<?, ?> store = heap.bytesStore();
            long off = heap.start();
            store.writeInt(off, 0x11223344);
            assertEquals(0x11223344, store.readInt(off));
            store.writeOrderedInt(off, 0x55667788);
            assertEquals(0x55667788, store.readInt(off));
        } finally {
            heap.releaseLast();
        }
    }
}

