/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests PointerBytesStore operations and capacity handling because correct
 * pointer management is essential to avoid memory corruption when wrapping
 * native addresses.
 */
@SuppressWarnings("MMOverusedWord") // bytes domain terminology
@DisplayName("Pointer bytes store operations and capacity checks")
public class PointerBytesStoreTest extends BytesTestCommon {

    @Test
    @DisplayName("pointer bytes preserve read and write limits")
    public void testWriteSetLimitRead() {
        final Bytes<?> data = Bytes.allocateDirect(14);
        data.write8bit("Test me again");
        data.writeLimit(data.readLimit()); // this breaks the check
        assertEquals("Test me again", data.read8bit(),
                "Read back string matches written value");
        data.releaseLast();
    }

    @Test
    @DisplayName("pointer store wraps native bytes address")
    public void testWrap() {
        final NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(10000);
        final PointerBytesStore pbs = BytesStore.nativePointer();
        try {
            pbs.set(nbs.addressForRead(nbs.start()), nbs.realCapacity());
            final long nanoTime = System.nanoTime();
            pbs.writeLong(0L, nanoTime);

            assertEquals(nanoTime, nbs.readLong(0L),
                    "Native store read matches written nano time");
        } finally {
            nbs.releaseLast();
            pbs.releaseLast();
        }
    }

    @Test
    @DisplayName("pointer store exposes write limit correctly")
    public void testWriteLimit() {
        final PointerBytesStore pbs = new PointerBytesStore();
        final Bytes<Void> wrapper = pbs.bytesForRead();
        pbs.set(NoBytesStore.NO_PAGE, 200);
        wrapper.writeLimit(pbs.capacity());
        assertEquals(pbs.capacity(), wrapper.writeLimit(),
                "Write limit reflects pointer bytes capacity");
        wrapper.releaseLast();
    }

    @Test
    @DisplayName("pointer store reads 8bit string correctly")
    public void testRead8BitString() {
        final Bytes<Void> bytesFixed = Bytes.allocateDirect(32);

        try {
            bytesFixed.write8bit("some data");
            final long addr = bytesFixed.addressForRead(bytesFixed.readPosition());
            final long len = bytesFixed.readRemaining();
            final PointerBytesStore pbs = new PointerBytesStore();
            pbs.set(addr, len);
            Bytes<Void> voidBytes = pbs.bytesForRead();
            assertEquals("some data", voidBytes.read8bit(),
                    "Read 8bit string matches stored content");
            voidBytes.releaseLast();
        } finally {
            bytesFixed.releaseLast();
        }
    }

    @Test
    @DisplayName("pointer store capacity reflects underlying type")
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

            assertEquals(fixedCap, pbs.capacity(),
                    "Pointer capacity equals fixed backing capacity");
            assertFalse(bytes.isElastic(),
                    "Pointer bytes remain non elastic for fixed store");

            bytes.clear();
            pbs.set(elasticAddr, bytesElastic.capacity());

            assertEquals(elasticCap, pbs.capacity(),
                    "Pointer capacity equals elastic backing capacity");
            expectException("the provided capacity of underlying looks like it may have come from an elastic bytes, " +
                    "please make sure you do not use PointerBytesStore with ElasticBytes since " +
                    "the address of the underlying store may change once it expands");
            assertFalse(bytes.isElastic(),
                    "Pointer bytes remain non elastic after elastic source");
            bytes.releaseLast();
        } finally {
            bytesFixed.releaseLast();

        }
    }
}
