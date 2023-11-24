/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.bytes.internal.HeapBytesStore;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class HeapByteStoreTest extends BytesTestCommon {
    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull HeapBytesStore hbs = HeapBytesStore.wrap("Hello".getBytes());
        @NotNull HeapBytesStore hbs2 = HeapBytesStore.wrap("Hello".getBytes());
        @NotNull HeapBytesStore hbs3 = HeapBytesStore.wrap("He!!o".getBytes());
        @NotNull HeapBytesStore hbs4 = HeapBytesStore.wrap("Hi".getBytes());
        assertEquals(hbs, hbs2);
        assertEquals(hbs2, hbs);
        assertNotEquals(hbs, hbs3);
        assertNotEquals(hbs3, hbs);
        assertNotEquals(hbs, hbs4);
        assertNotEquals(hbs4, hbs);
    }

    @Test
    public void testElasticBytesEnsuringCapacity() {
        Bytes<?> bytes = Bytes.elasticHeapByteBuffer();
        long initialCapacity = bytes.realCapacity();
        bytes.clearAndPad(bytes.realCapacity() + 128);
        // ensure this succeeds even though we are above the real capacity - this should trigger resize
        bytes.prewriteInt(1);
        assertTrue(bytes.realCapacity()> initialCapacity);
    }

    @Test
    public void testWriteOnHeap() throws Exception {
        doTestWrite(() -> ByteBuffer.allocate(128));
    }

    @Test
    public void testWriteDirect() throws Exception {
        doTestWrite(() -> ByteBuffer.allocateDirect(128));
    }

    private void doTestWrite(Callable<ByteBuffer> generator) throws Exception {
        final Bytes<?> data = Bytes.elasticHeapByteBuffer(128);
        ByteBuffer buffer = generator.call();

        for (byte c = ' '; c < '`'; c++) {
            data.writeChar((char)c);
            buffer.put(c);
        }

        HeapBytesStore heapBytesStore = (HeapBytesStore) data.bytesStore();
        heapBytesStore.write(16, buffer, 32, 8);
        for (int i = 0; i < 16; i++)
            assertEquals(i + ' ', heapBytesStore.readByte(i));

        for (int i = 16; i < 24; i++)
            assertEquals(i + '0', heapBytesStore.readByte(i));

        for (int i = 24; i < 32; i++)
            assertEquals(i + ' ', heapBytesStore.readByte(i));
    }
}
