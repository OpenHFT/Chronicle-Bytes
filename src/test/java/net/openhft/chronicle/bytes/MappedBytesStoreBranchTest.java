/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MappedBytesStore branch coverage.
 */
class MappedBytesStoreBranchTest extends BytesTestCommon implements ReferenceOwner {

    @TempDir
    File tempDir;

    private MappedFile mappedFile;
    private MappedBytesStore store;

    @BeforeEach
    void setUp() throws IOException {
        File file = new File(tempDir, "test-mapped.dat");
        mappedFile = MappedFile.mappedFile(file, 4096);
        store = mappedFile.acquireByteStore(this, 0);
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.release(this);
        }
        if (mappedFile != null) {
            mappedFile.releaseLast();
        }
    }

    @Test
    @DisplayName("start returns mapped file start offset")
    void startReturnsPosition() {
        assertEquals(0, store.start(), "mapped store start offset equals zero");
    }

    @Test
    @DisplayName("readPosition matches start offset")
    void readPositionEqualsStart() {
        assertEquals(store.start(), store.readPosition(), "readPosition equals start offset");
    }

    @Test
    @DisplayName("inside(offset) honours safe limit boundary")
    void insideChecksSafeLimit() {
        assertTrue(store.inside(0), "offset 0 is inside safe limit");
        assertTrue(store.inside(100), "offset 100 is inside safe limit");
    }

    @Test
    @DisplayName("inside(offset, size) honours range within limit")
    void insideChecksRange() {
        assertTrue(store.inside(0, 100), "range 0-100 is inside safe limit");
    }

    @Test
    @DisplayName("safeLimit returns positive boundary value")
    void safeLimitReturnsBoundary() {
        long safeLimit = store.safeLimit();
        assertTrue(safeLimit > 0, "safeLimit returns positive boundary value");
    }

    @Test
    @DisplayName("translate converts offset to local position")
    void translateConvertsOffset() {
        long translated = store.translate(0);
        assertTrue(translated >= -store.start(), "translated offset is within start range");
    }

    @Test
    @DisplayName("underlyingCapacity matches mapped file capacity")
    void underlyingCapacityReturnsFileCapacity() {
        assertEquals(mappedFile.capacity(), store.underlyingCapacity(),
                "underlyingCapacity equals mapped file capacity");
    }

    @Test
    @DisplayName("bytesForRead creates readable view over store")
    void bytesForReadCreatesView() {
        store.writeLong(0, 0x1234567890ABCDEFL);
        Bytes<?> bytes = store.bytesForRead();
        try {
            assertEquals(0x1234567890ABCDEFL, bytes.readLong(), "bytesForRead returns written long value");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("bytesForWrite creates writable view over store")
    void bytesForWriteCreatesView() {
        Bytes<?> bytes = store.bytesForWrite();
        try {
            bytes.writeLong(0x1234567890ABCDEFL);
            assertEquals(0x1234567890ABCDEFL, store.readLong(0), "bytesForWrite writeLong updates store data");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("readByte reads from mapped memory")
    void readByteFromMapped() {
        store.writeByte(0, (byte) 0x42);
        assertEquals((byte) 0x42, store.readByte(0), "readByte returns written byte value");
    }

    @Test
    @DisplayName("writeByte writes into mapped memory")
    void writeByteToMapped() {
        store.writeByte(10, (byte) 0x55);
        assertEquals((byte) 0x55, store.readByte(10), "writeByte stores written byte value");
    }

    @Test
    @DisplayName("writeShort writes into mapped memory")
    void writeShortToMapped() {
        store.writeShort(0, (short) 0x1234);
        assertEquals((short) 0x1234, store.readShort(0), "writeShort stores written short value");
    }

    @Test
    @DisplayName("writeInt writes into mapped memory")
    void writeIntToMapped() {
        store.writeInt(0, 0x12345678);
        assertEquals(0x12345678, store.readInt(0), "writeInt stores written int value");
    }

    @Test
    @DisplayName("writeLong writes into mapped memory")
    void writeLongToMapped() {
        store.writeLong(0, 0x123456789ABCDEF0L);
        assertEquals(0x123456789ABCDEF0L, store.readLong(0), "writeLong stores written long value");
    }

    @Test
    @DisplayName("writeFloat writes into mapped memory")
    void writeFloatToMapped() {
        store.writeFloat(0, 3.14f);
        assertEquals(3.14f, store.readFloat(0), 0.001f, "writeFloat stores written float value");
    }

    @Test
    @DisplayName("writeDouble writes into mapped memory")
    void writeDoubleToMapped() {
        store.writeDouble(0, 3.14159);
        assertEquals(3.14159, store.readDouble(0), 0.00001, "writeDouble stores written double value");
    }

    @Test
    @DisplayName("writeOrderedInt stores value with ordering")
    void writeOrderedIntToMapped() {
        store.writeOrderedInt(0, 0xDEADBEEF);
        assertEquals(0xDEADBEEF, store.readInt(0), "readInt returns ordered int value");
    }

    @Test
    @DisplayName("writeOrderedLong stores value with ordering")
    void writeOrderedLongToMapped() {
        store.writeOrderedLong(0, 0xDEADBEEFCAFEBABEL);
        assertEquals(0xDEADBEEFCAFEBABEL, store.readLong(0), "readLong returns ordered long value");
    }

    @Test
    @DisplayName("writeVolatileByte stores value with volatility")
    void writeVolatileByteToMapped() {
        store.writeVolatileByte(0, (byte) 0x77);
        assertEquals((byte) 0x77, store.readVolatileByte(0), "readVolatileByte returns volatile byte value");
    }

    @Test
    @DisplayName("writeVolatileShort stores value with volatility")
    void writeVolatileShortToMapped() {
        store.writeVolatileShort(0, (short) 0x1234);
        assertEquals((short) 0x1234, store.readVolatileShort(0), "readVolatileShort returns volatile short value");
    }

    @Test
    @DisplayName("writeVolatileInt stores value with volatility")
    void writeVolatileIntToMapped() {
        store.writeVolatileInt(0, 0x12345678);
        assertEquals(0x12345678, store.readVolatileInt(0), "readVolatileInt returns volatile int value");
    }

    @Test
    @DisplayName("writeVolatileLong stores value with volatility")
    void writeVolatileLongToMapped() {
        store.writeVolatileLong(0, 0x123456789ABCDEF0L);
        assertEquals(0x123456789ABCDEF0L, store.readVolatileLong(0), "readVolatileLong returns volatile long value");
    }

    @Test
    @DisplayName("compareAndSwapInt updates mapped value atomically")
    void compareAndSwapIntWorks() {
        store.writeInt(0, 100);
        assertTrue(store.compareAndSwapInt(0, 100, 200), "compareAndSwapInt returns true on match");
        assertEquals(200, store.readInt(0), "compareAndSwapInt updates value to 200");
    }

    @Test
    @DisplayName("compareAndSwapLong updates mapped value atomically")
    void compareAndSwapLongWorks() {
        store.writeLong(0, 100L);
        assertTrue(store.compareAndSwapLong(0, 100L, 200L), "compareAndSwapLong returns true on match");
        assertEquals(200L, store.readLong(0), "compareAndSwapLong updates value to 200");
    }

    @Test
    @DisplayName("zeroOut fills selected range with zeros")
    void zeroOutFillsWithZeros() {
        store.writeLong(0, 0xFFFFFFFFFFFFFFFFL);
        store.zeroOut(0, 8);
        assertEquals(0L, store.readLong(0), "zeroOut clears range to zero");
    }

    @Test
    @DisplayName("write(byte[]) copies bytes into mapped store")
    void writeFromByteArray() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        store.write(0, data, 0, 5);
        assertEquals(1, store.readByte(0), "byte array element 0 equals 1");
        assertEquals(5, store.readByte(4), "byte array element 4 equals 5");
    }

    @Test
    @DisplayName("syncMode stores and returns selected mode")
    void syncModeGetSet() {
        store.syncMode(SyncMode.ASYNC);
        assertEquals(SyncMode.ASYNC, store.syncMode(), "syncMode returns ASYNC after set");

        store.syncMode(SyncMode.SYNC);
        assertEquals(SyncMode.SYNC, store.syncMode(), "syncMode returns SYNC after set");
    }

    @Test
    @DisplayName("syncMode uses NONE when set to null")
    void syncModeNullReturnsNone() {
        store.syncMode(null);
        assertEquals(SyncMode.NONE, store.syncMode(), "syncMode returns NONE when set to null");
    }

    @Test
    @DisplayName("syncUpTo does not sync in NONE mode")
    void syncUpToNoneMode() {
        store.syncMode(SyncMode.NONE);
        store.writeLong(0, 12345L);
        // Should not throw and should not actually sync
        store.syncUpTo(100);
    }

    @Test
    @DisplayName("syncUpTo ignores position below sync length")
    void syncUpToLessThanSyncLength() {
        store.syncMode(SyncMode.ASYNC);
        store.writeLong(0, 12345L);
        // First sync to set syncLength
        store.syncUpTo(4096);
        // Second sync with smaller position should be no-op
        store.syncUpTo(100);
    }

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsWithSameInstance() {
        assertEquals(store, store, "equals returns true for same instance");
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsWithNull() {
        assertNotEquals(null, store, "equals returns false for null");
    }

    @Test
    @DisplayName("equals returns false for different type")
    void equalsWithDifferentClass() {
        assertNotEquals("string", store, "equals returns false for different type");
    }

    @Test
    @DisplayName("hashCode remains consistent across calls")
    void hashCodeConsistent() {
        int hash1 = store.hashCode();
        int hash2 = store.hashCode();
        assertEquals(hash1, hash2, "hashCode returns same value across calls");
    }

    @Test
    @DisplayName("lock acquires valid file lock")
    void lockAcquiresFileLock() throws IOException {
        FileLock lock = store.lock(0, 100, false);
        try {
            assertNotNull(lock, "lock returns non-null FileLock");
            assertTrue(lock.isValid(), "lock is valid after acquire");
        } finally {
            lock.release();
        }
    }

    @Test
    @DisplayName("tryLock attempts to acquire file lock")
    void tryLockAttemptsLock() throws IOException {
        FileLock lock = store.tryLock(0, 100, false);
        if (lock != null) {
            try {
                assertTrue(lock.isValid(), "tryLock returns valid FileLock when acquired");
            } finally {
                lock.release();
            }
        }
        // lock may be null if not acquired, which is valid behaviour
    }

    @Test
    @DisplayName("appendUtf8 writes UTF-8 characters")
    void appendUtf8WritesChars() {
        char[] chars = {'H', 'e', 'l', 'l', 'o'};
        long pos = store.appendUtf8(0, chars, 0, 5);
        assertTrue(pos >= 5, "appendUtf8 returns end position after write");
        assertEquals('H', (char) store.readByte(0), "first character equals H");
    }
}
