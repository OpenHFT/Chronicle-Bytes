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

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.junit.Assert.*;

public class PointerBytesStoreTest extends BytesTestCommon {

    @Test
    public void testWriteSetLimitRead() {
        final Bytes<?> data = Bytes.allocateDirect(14);
        data.write8bit("Test me again");
        data.writeLimit(data.readLimit()); // this breaks the check
        assertEquals(data.read8bit(), "Test me again");
    }

    @Test
    public void testWrap() {
        final NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(10000);
        final PointerBytesStore pbs = BytesStore.nativePointer();
        try {
            pbs.set(nbs.addressForRead(nbs.start()), nbs.realCapacity());
            final long nanoTime = System.nanoTime();
            pbs.writeLong(0L, nanoTime);

            assertEquals(nanoTime, nbs.readLong(0L));
        } finally {
            nbs.releaseLast();
            pbs.releaseLast();
        }
    }

    @Test
    public void testWriteLimit() {
        final PointerBytesStore pbs = new PointerBytesStore();
        final Bytes<Void> wrapper = pbs.bytesForRead();
        pbs.set(NoBytesStore.NO_PAGE, 200);
        wrapper.writeLimit(pbs.capacity());
        assertEquals(pbs.capacity(), wrapper.writeLimit());
    }

    @Test
    public void testRead8BitString() {
        final Bytes<Void> bytesFixed = Bytes.allocateDirect(32);

        try {
            bytesFixed.write8bit("some data");
            final long addr = bytesFixed.addressForRead(bytesFixed.readPosition());
            final long len = bytesFixed.readRemaining();
            final PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(addr, len);
            Assertions.assertEquals(pbs.bytesForRead().read8bit(), "some data");
        } finally {
            bytesFixed.releaseLast();
        }
    }

    @Test
    public void testUnderlyingCapacityAndType() {
        final Bytes<Void> bytesFixed = Bytes.allocateDirect(32);
        final Bytes<Void> bytesElastic = Bytes.allocateElasticDirect();
        final PointerBytesStore pbs = new PointerBytesStore();

        try {

            bytesFixed.write8bit("bytesFixed");
            final long fixedAddr = bytesFixed.addressForRead(bytesFixed.readPosition());
            final long fixedCap = bytesFixed.capacity();

            bytesElastic.write8bit("bytesElastic");
            final long elasticAddr = bytesElastic.addressForRead(bytesElastic.readPosition());
            final long elasticCap = bytesElastic.capacity();

            pbs.set(fixedAddr, fixedCap);
            Bytes<Void> bytes = pbs.bytesForRead();

            assertEquals(pbs.capacity(), fixedCap);
            assertFalse(bytes.isElastic());

            bytes.clear();
            pbs.set(elasticAddr, bytesElastic.capacity());

            assertEquals(pbs.capacity(), elasticCap);
            expectException("the provided capacity of underlying looks like it may have come from an elastic bytes, " +
                    "please make sure you do not use PointerBytesStore with ElasticBytes since " +
                    "the address of the underlying store may change once it expands");
            assertFalse(bytes.isElastic());
        } finally {
            bytesFixed.releaseLast();

        }
    }
}