/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * User: peter.lawrey Date: 24/12/13 Time: 19:43
 */
/*
buffers 128 KB took an average of 18,441 ns for heap ByteBuffer, 33,683 ns for direct ByteBuffer and 1,761 for DirectStore
buffers 128 KB took an average of 13,062 ns for heap ByteBuffer, 17,855 ns for direct ByteBuffer and 903 for DirectStore
buffers 128 KB took an average of 12,809 ns for heap ByteBuffer, 21,602 ns for direct ByteBuffer and 922 for DirectStore
buffers 128 KB took an average of 10,768 ns for heap ByteBuffer, 21,444 ns for direct ByteBuffer and 894 for DirectStore
buffers 128 KB took an average of 8,739 ns for heap ByteBuffer, 22,684 ns for direct ByteBuffer and 890 for DirectStore
 */
public class AllocationRatesTest extends BytesTestCommon {
    private static final int BATCH = 10;
    private static final int BUFFER_SIZE = 128 * 1024;
    private static final int ALLOCATIONS = 10000;

    @Test
    public void compareAllocationRates() {
        for (int i = 4; i >= 0; i--) {
            long timeHBB = timeHeapByteBufferAllocations();
            long timeDBB = timeDirectByteBufferAllocations();
            long timeDS = timeDirectStoreAllocations();
            if (i == 0)
                System.out.printf("buffers %d KB took an average of %,d ns for heap ByteBuffer, %,d ns for direct ByteBuffer and %,d for DirectStore%n",
                        BUFFER_SIZE / 1024, timeHBB / ALLOCATIONS, timeDBB / ALLOCATIONS, timeDS / ALLOCATIONS);
        }
    }

    private long timeHeapByteBufferAllocations() {
        long start = System.nanoTime();
        for (int i = 0; i < ALLOCATIONS; i += BATCH) {
            @NotNull ByteBuffer[] bb = new ByteBuffer[BATCH];
            for (int j = 0; j < BATCH; j++)
                bb[j] = ByteBuffer.allocate(BUFFER_SIZE);
        }
        return System.nanoTime() - start;
    }

    private long timeDirectByteBufferAllocations() {
        long start = System.nanoTime();
        for (int i = 0; i < ALLOCATIONS; i += BATCH) {
            @NotNull ByteBuffer[] bb = new ByteBuffer[BATCH];
            for (int j = 0; j < BATCH; j++)
                bb[j] = ByteBuffer.allocateDirect(BUFFER_SIZE);
        }
        return System.nanoTime() - start;
    }

    @SuppressWarnings("rawtypes")
    private long timeDirectStoreAllocations() {
        long start = System.nanoTime();
        for (int i = 0; i < ALLOCATIONS; i += BATCH) {
            @NotNull BytesStore[] ds = new BytesStore[BATCH];
            for (int j = 0; j < BATCH; j++)
                ds[j] = BytesStore.lazyNativeBytesStoreWithFixedCapacity(BUFFER_SIZE);
            for (int j = 0; j < BATCH; j++) {
                ds[j].releaseLast();
                assertEquals(0, ds[j].refCount());
            }
        }
        return System.nanoTime() - start;
    }
}
