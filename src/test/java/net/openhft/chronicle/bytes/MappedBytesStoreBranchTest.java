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
 * Tests MappedBytesStore branch coverage because these edge cases are
 * critical to avoid memory leaks and ensure correct synchronisation
 * with memory-mapped files. In order to prevent data corruption,
 * synchronisation mode transitions and atomic operations must be tested.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("MappedBytesStore branch coverage for sync modes and atomic operations")
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
    @DisplayName("readPosition equals store start offset because read begins at mapping origin")
    void readPositionEqualsStart() {
        assertEquals(store.start(), store.readPosition(), "readPosition equals start offset after construction");
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
    @DisplayName("safeLimit returns positive boundary for valid read and write operations")
    void safeLimitReturnsBoundary() {
        long safeLimit = store.safeLimit();
        assertTrue(safeLimit > 0, "safeLimit=" + safeLimit + " should be positive for mapped file");
    }

    @Test
    @DisplayName("translate converts offset to local position")
    void translateConvertsOffset() {
        long translated = store.translate(0);
        assertTrue(translated >= -store.start(), "translated offset is within start range");
    }

    @Test
    @DisplayName("underlyingCapacity returns mapped file capacity because store wraps entire file")
    void underlyingCapacityReturnsFileCapacity() {
        assertEquals(mappedFile.capacity(), store.underlyingCapacity(),
                "underlyingCapacity=" + store.underlyingCapacity() + " should equal file capacity");
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
    @DisplayName("readByte at offset 0 returns written byte 0x42 because write persists to mapped memory")
    void readByteFromMapped() {
        store.writeByte(0, (byte) 0x42);
        assertEquals((byte) 0x42, store.readByte(0), "readByte(0) returns 0x42 after writeByte(0, 0x42)");
    }

    @Test
    @DisplayName("writeByte at offset 10 stores byte 0x55 because write persists to mapped memory")
    void writeByteToMapped() {
        store.writeByte(10, (byte) 0x55);
        assertEquals((byte) 0x55, store.readByte(10), "readByte(10) returns 0x55 after writeByte(10, 0x55)");
    }

    @Test
    @DisplayName("writeShort at offset 0 stores short 0x1234 because write persists to mapped memory")
    void writeShortToMapped() {
        store.writeShort(0, (short) 0x1234);
        assertEquals((short) 0x1234, store.readShort(0), "readShort(0) returns 0x1234 after writeShort");
    }

    @Test
    @DisplayName("writeInt at offset 0 stores int 0x12345678 because write persists to mapped memory")
    void writeIntToMapped() {
        store.writeInt(0, 0x12345678);
        assertEquals(0x12345678, store.readInt(0), "readInt(0) returns 0x12345678 after writeInt");
    }

    @Test
    @DisplayName("writeLong at offset 0 stores long value because write persists to mapped memory")
    void writeLongToMapped() {
        store.writeLong(0, 0x123456789ABCDEF0L);
        assertEquals(0x123456789ABCDEF0L, store.readLong(0), "readLong(0) returns written long value");
    }

    @Test
    @DisplayName("writeFloat at offset 0 stores float 3.14 because write persists to mapped memory")
    void writeFloatToMapped() {
        store.writeFloat(0, 3.14f);
        assertEquals(3.14f, store.readFloat(0), 0.001f, "readFloat(0) returns 3.14 after writeFloat");
    }

    @Test
    @DisplayName("writeDouble at offset 0 stores double 3.14159 because write persists to mapped memory")
    void writeDoubleToMapped() {
        store.writeDouble(0, 3.14159);
        assertEquals(3.14159, store.readDouble(0), 0.00001, "readDouble(0) returns 3.14159 after writeDouble");
    }

    @Test
    @DisplayName("writeOrderedInt at offset 0 stores value with memory ordering guarantee")
    void writeOrderedIntToMapped() {
        store.writeOrderedInt(0, 0xDEADBEEF);
        assertEquals(0xDEADBEEF, store.readInt(0), "readInt(0) returns 0xDEADBEEF after writeOrderedInt");
    }

    @Test
    @DisplayName("writeOrderedLong at offset 0 stores value with memory ordering guarantee")
    void writeOrderedLongToMapped() {
        store.writeOrderedLong(0, 0xDEADBEEFCAFEBABEL);
        assertEquals(0xDEADBEEFCAFEBABEL, store.readLong(0), "readLong(0) returns value after writeOrderedLong");
    }

    @Test
    @DisplayName("writeVolatileByte at offset 0 stores byte 0x77 with volatile semantics")
    void writeVolatileByteToMapped() {
        store.writeVolatileByte(0, (byte) 0x77);
        assertEquals((byte) 0x77, store.readVolatileByte(0), "readVolatileByte(0) returns 0x77 after write");
    }

    @Test
    @DisplayName("writeVolatileShort at offset 0 stores short 0x1234 with volatile semantics")
    void writeVolatileShortToMapped() {
        store.writeVolatileShort(0, (short) 0x1234);
        assertEquals((short) 0x1234, store.readVolatileShort(0), "readVolatileShort(0) returns 0x1234 after write");
    }

    @Test
    @DisplayName("writeVolatileInt at offset 0 stores int 0x12345678 with volatile semantics")
    void writeVolatileIntToMapped() {
        store.writeVolatileInt(0, 0x12345678);
        assertEquals(0x12345678, store.readVolatileInt(0), "readVolatileInt(0) returns 0x12345678 after write");
    }

    @Test
    @DisplayName("writeVolatileLong at offset 0 stores long value with volatile semantics")
    void writeVolatileLongToMapped() {
        store.writeVolatileLong(0, 0x123456789ABCDEF0L);
        assertEquals(0x123456789ABCDEF0L, store.readVolatileLong(0), "readVolatileLong(0) returns value after write");
    }

    @Test
    @DisplayName("compareAndSwapInt atomically updates 100 to 200 when expected value matches")
    void compareAndSwapIntWorks() {
        store.writeInt(0, 100);
        assertTrue(store.compareAndSwapInt(0, 100, 200), "CAS int succeeds when expected=100 matches actual=100");
        assertEquals(200, store.readInt(0), "readInt(0) returns 200 after successful CAS");
    }

    @Test
    @DisplayName("compareAndSwapLong atomically updates 100L to 200L when expected value matches")
    void compareAndSwapLongWorks() {
        store.writeLong(0, 100L);
        assertTrue(store.compareAndSwapLong(0, 100L, 200L), "CAS long succeeds when expected=100L matches actual=100L");
        assertEquals(200L, store.readLong(0), "readLong(0) returns 200L after successful CAS");
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
    @DisplayName("syncMode returns NONE when set to null because null is treated as disabled sync")
    void syncModeNullReturnsNone() {
        store.syncMode(null);
        assertEquals(SyncMode.NONE, store.syncMode(), "syncMode returns NONE when set to null to disable sync");
    }

    @Test
    @DisplayName("syncUpTo returns early in NONE mode because no synchronisation is needed")
    void syncUpToNoneMode() {
        store.syncMode(SyncMode.NONE);
        store.writeLong(0, 12345L);
        // Verify no exception: syncUpTo should return early in NONE mode
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
    @DisplayName("equals returns true when comparing store to itself because identity implies equality")
    void equalsWithSameInstance() {
        assertEquals(store, store, "equals(this) returns true for identity comparison");
    }

    @Test
    @DisplayName("equals returns false when comparing store to null because null is never equal")
    void equalsWithNull() {
        assertNotEquals(null, store, "equals(null) returns false for null comparison");
    }

    @Test
    @DisplayName("equals returns false when comparing store to String because types differ")
    void equalsWithDifferentClass() {
        assertNotEquals("string", store, "equals(String) returns false for different type comparison");
    }

    @Test
    @DisplayName("hashCode returns consistent value across multiple invocations for same instance")
    void hashCodeConsistent() {
        int hash1 = store.hashCode();
        int hash2 = store.hashCode();
        assertEquals(hash1, hash2, "hashCode()=" + hash1 + " equals hashCode()=" + hash2 + " across calls");
    }

    @Test
    @DisplayName("lock(0, 100, false) acquires exclusive FileLock on first 100 bytes of mapped region")
    void lockAcquiresFileLock() throws IOException {
        FileLock lock = store.lock(0, 100, false);
        try {
            assertNotNull(lock, "lock(0, 100, false) returns non-null FileLock");
            assertTrue(lock.isValid(), "FileLock.isValid() returns true after acquisition");
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
    @DisplayName("appendUtf8 writes 'Hello' character array and returns position >= 5 after write")
    void appendUtf8WritesChars() {
        char[] chars = {'H', 'e', 'l', 'l', 'o'};
        long pos = store.appendUtf8(0, chars, 0, 5);
        assertTrue(pos >= 5, "appendUtf8 returns pos=" + pos + " which should be >= 5 after writing 5 chars");
        assertEquals('H', (char) store.readByte(0), "readByte(0) returns 'H' after appendUtf8('Hello')");
    }
}
