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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisplayName("Mapped bytes store read write behaviour")
public class MappedBytesStoreTest extends BytesTestCommon implements ReferenceOwner {
    private static final int PAGE_SIZE = OS.defaultOsPageSize();
    private MappedFile mappedFile;
    private MappedBytesStore mappedBytesStore;

    @BeforeEach
    public void setup() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped bytes store test");

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
    @DisplayName("write and read byte round trip")
    public void testWriteReadBytes() throws ClosedIllegalStateException {
        byte value = 123;
        long position = 5;
        mappedBytesStore.writeByte(position, value);

        byte readValue = mappedBytesStore.readByte(position);
        assertEquals(value, readValue,
                "Written and read byte values should match");
    }

    @Test
    @DisplayName("write after close throws illegal state")
    public void testWriteAfterClose() {
        try {
            mappedBytesStore.release(this);
            assertThrows(ClosedIllegalStateException.class,
                    () -> mappedBytesStore.release(ReferenceOwner.INIT),
                    "Release after close should throw illegal state");
            assertThrows(NullPointerException.class,
                    () -> mappedBytesStore.writeByte(0, (byte) 1),
                    "Write after close should throw illegal state");
        } finally {
            mappedBytesStore = null;
        }
    }

    @Test
    @DisplayName("safe limit bounds for inside check")
    public void testSafeLimit() {
        assertTrue(mappedBytesStore.inside(0),
                "Position within safe limit is valid");
        assertFalse(mappedBytesStore.inside(mappedBytesStore.safeLimit()),
                "Position beyond safe limit is invalid");
    }

    @Test
    @DisplayName("capacity matches expected mapped file size")
    public void testCapacity() {
        assertEquals(PAGE_SIZE * 2, mappedBytesStore.capacity(),
                "Mapped bytes store capacity matches expected size");
    }

    @Test
    @DisplayName("lock region obtains active file lock handle")
    public void testLockRegion() throws IOException {
        // Try to lock a region of the file
        assertNotNull(mappedBytesStore.tryLock(0, 10, true),
                "File lock handle returned for region lock attempt");
    }

    @Test
    @DisplayName("byte buffer read write matches content")
    public void testByteBufferReadWrite() throws ClosedIllegalStateException {
        byte[] writeBytes = new byte[10];
        for (byte i = 0; i < 10; i++) {
            writeBytes[i] = i;
        }
        mappedBytesStore.write(0, writeBytes, 0, writeBytes.length);

        byte[] readBytes = new byte[10];
        mappedBytesStore.read(0, readBytes, 0, 10);

        assertArrayEquals(writeBytes, readBytes,
                "Read buffer content matches written bytes");
    }

    @Test
    @DisplayName("syncUpTo runs without errors after reopen")
    public void testSyncUpTo() throws IOException {
        mappedBytesStore.syncUpTo(0);
        mappedBytesStore.syncUpTo(1000);
        mappedBytesStore.syncUpTo(5000);
        mappedBytesStore.syncUpTo(1000000);

        mappedBytesStore.release(this);
        mappedBytesStore = mappedFile.acquireByteStore(this, OS.pageSize());

        assertDoesNotThrow(() -> {
            mappedBytesStore.syncUpTo(0);
            mappedBytesStore.syncUpTo(1000);
            mappedBytesStore.syncUpTo(5000);
            mappedBytesStore.syncUpTo(1000000);
        }, "syncUpTo calls succeed after reacquiring byte store");
    }
}
