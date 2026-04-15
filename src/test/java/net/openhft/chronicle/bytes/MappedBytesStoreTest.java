/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class MappedBytesStoreTest extends BytesTestCommon implements ReferenceOwner {
    private static final int PAGE_SIZE = OS.defaultOsPageSize();
    private MappedFile mappedFile;
    private MappedBytesStore mappedBytesStore;

    @BeforeEach
    public void setup() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        String filePath = OS.getTarget() + "/test" + System.nanoTime() + ".deleteme";
        mappedFile = MappedFile.mappedFile(filePath, PAGE_SIZE, PAGE_SIZE);
        mappedBytesStore = mappedFile.acquireByteStore(this, 0);
        new File(filePath).deleteOnExit();
    }

    @AfterEach
    public void tearDown() {
        if (mappedBytesStore != null)
            mappedBytesStore.release(this);
        Closeable.closeQuietly(mappedFile);
    }

    @Test
    public void testWriteReadBytes() throws ClosedIllegalStateException {
        byte value = 123;
        long position = 5;
        mappedBytesStore.writeByte(position, value);

        byte readValue = mappedBytesStore.readByte(position);
        assertEquals(value, readValue, "Written and read values should be equal");
    }

    @Test
    public void testWriteAfterClose() {
        assertThrows(IllegalStateException.class, () -> {
            try {
                mappedBytesStore.release(this);
                mappedBytesStore.release(ReferenceOwner.INIT);
                mappedBytesStore.writeByte(0, (byte) 1);
            } finally {
                mappedBytesStore = null;
            }
        });
    }

    @Test
    public void testSafeLimit() {
        assertTrue(mappedBytesStore.inside(0), "Position within safe limit should be valid");
        assertFalse(mappedBytesStore.inside(mappedBytesStore.safeLimit()), "Position beyond safe limit should be invalid");
    }

    @Test
    public void testCapacity() {
        assertEquals(PAGE_SIZE * 2, mappedBytesStore.capacity(), "The capacities should match");
    }

    @Test
    public void testLockRegion() throws IOException {
        // Try to lock a region of the file
        assertNotNull(mappedBytesStore.tryLock(0, 10, true), "Lock should be obtained");
    }

    @Test
    public void testByteBufferReadWrite() throws ClosedIllegalStateException {
        byte[] writeBytes = new byte[10];
        for (byte i = 0; i < 10; i++) {
            writeBytes[i] = i;
        }
        mappedBytesStore.write(0, writeBytes, 0, writeBytes.length);

        byte[] readBytes = new byte[10];
        mappedBytesStore.read(0, readBytes, 0, 10);

        assertArrayEquals(writeBytes, readBytes, "Buffer content should match");
    }

    @Test
    public void testSyncUpTo() throws IOException {
        mappedBytesStore.syncUpTo(0);
        mappedBytesStore.syncUpTo(1000);
        mappedBytesStore.syncUpTo(5000);
        mappedBytesStore.syncUpTo(1000000);

        mappedBytesStore.release(this);
        mappedBytesStore = mappedFile.acquireByteStore(this, OS.pageSize());

        mappedBytesStore.syncUpTo(0);
        mappedBytesStore.syncUpTo(1000);
        mappedBytesStore.syncUpTo(5000);
        mappedBytesStore.syncUpTo(1000000);
        assertTrue(true); // If no exceptions, the test passes
    }
}
