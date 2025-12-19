/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointerBytesStoreTest extends BytesTestCommon {

    @Test
    public void testWriteSetLimitRead() {
        final Bytes<?> data = Bytes.allocateDirect(14);
        data.write8bit("Test me again");
        data.writeLimit(data.readLimit()); // this breaks the check
        assertEquals("Test me again", data.read8bit(), "read8bit should return complete written string after write limit adjustment");
        data.releaseLast();
    }

    @Test
    public void testWrap() {
        final NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(10000);
        final PointerBytesStore pbs = BytesStore.nativePointer();
        try {
            pbs.set(nbs.addressForRead(nbs.start()), nbs.realCapacity());
            final long nanoTime = System.nanoTime();
            pbs.writeLong(0L, nanoTime);

            assertEquals(nanoTime, nbs.readLong(0L), "native store should read same long value written via pointer bytes store");
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
        assertEquals(pbs.capacity(), wrapper.writeLimit(), "wrapper write limit should match pointer bytes store capacity");
        wrapper.releaseLast();
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
            Bytes<Void> voidBytes = pbs.bytesForRead();
            assertEquals("some data", voidBytes.read8bit(), "pointer bytes store should read 8-bit string from direct bytes address");
            voidBytes.releaseLast();
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

            assertEquals(pbs.capacity(), fixedCap, "pointer bytes store capacity should match fixed bytes capacity");
            assertFalse(bytes.isElastic(), "elasticity should be false");

            bytes.clear();
            pbs.set(elasticAddr, bytesElastic.capacity());

            assertEquals(pbs.capacity(), elasticCap, "pointer bytes store capacity should match elastic bytes capacity");
            expectException("the provided capacity of underlying looks like it may have come from an elastic bytes, " +
                    "please make sure you do not use PointerBytesStore with ElasticBytes since " +
                    "the address of the underlying store may change once it expands");
            assertFalse(bytes.isElastic(), "elasticity should be false");
            bytes.releaseLast();
        } finally {
            bytesFixed.releaseLast();

        }
    }
}
