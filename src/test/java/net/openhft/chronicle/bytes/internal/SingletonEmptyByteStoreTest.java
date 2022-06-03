/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.RandomDataOutput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.function.ObjLongConsumer;

import static net.openhft.chronicle.bytes.Bytes.elasticByteBuffer;
import static net.openhft.chronicle.bytes.internal.SingletonEmptyByteStore.INSTANCE;
import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.jupiter.api.Assertions.*;

class SingletonEmptyByteStoreTest {

    @Test
    void reserve() {
        assertDoesNotThrow(() ->
                INSTANCE.reserve(INIT)
        );
    }

    @Test
    void release() {
        assertDoesNotThrow(() ->
                INSTANCE.release(INIT)
        );
    }

    @Test
    void releaseLast() {
        assertDoesNotThrow(() ->
                INSTANCE.releaseLast()
        );
    }

    @Test
    void releaseLastArg() {
        assertDoesNotThrow(() ->
                INSTANCE.releaseLast(INIT)
        );
    }

    @Test
    void refCount() {
        assertEquals(1, INSTANCE.refCount());
    }

    @Test
    void tryReserve() {
        assertFalse(INSTANCE.tryReserve(INIT));
    }

    @Test
    void reservedBy() {
        assertTrue(INSTANCE.reservedBy(INIT));
    }

    @Test
    void writeByteInt() {
        assertThrowsUOE(() -> INSTANCE.writeByte(0, 0));
    }

    @Test
    void writeByte() {
        assertThrowsUOE(() -> INSTANCE.writeByte(0, (byte) 0));
    }

    @Test
    void writeShort() {
        assertThrowsUOE(() -> INSTANCE.writeShort(0, (short) 0));
    }

    @Test
    void writeInt() {
        assertThrowsUOE(() -> INSTANCE.writeInt(0, 0));
    }

    @Test
    void writeOrderedInt() {
        assertThrowsUOE(() -> INSTANCE.writeOrderedInt(0, 0));
    }

    @Test
    void writeLong() {
        assertThrowsUOE(() -> INSTANCE.writeLong(0, 0));
    }

    @Test
    void writeOrderedLong() {
        assertThrowsUOE(() -> INSTANCE.writeOrderedLong(0, 0L));
    }

    @Test
    void writeFloat() {
        assertThrowsUOE(() -> INSTANCE.writeFloat(0, 0.0f));
    }

    @Test
    void writeDouble() {
        assertThrowsUOE(() -> INSTANCE.writeDouble(0, 0.0d));
    }

    @Test
    void writeVolatileByte() {
        assertThrowsUOE(() -> INSTANCE.writeVolatileByte(0, (byte) 0));
    }

    @Test
    void writeVolatileShort() {
        assertThrowsUOE(() -> INSTANCE.writeVolatileShort(0, (short) 0));
    }

    @Test
    void writeVolatileInt() {
        assertThrowsUOE(() -> INSTANCE.writeVolatileInt(0, 0));
    }

    @Test
    void writeVolatileLong() {
        assertThrowsUOE(() -> INSTANCE.writeVolatileLong(0, 0L));
    }

    @Test
    void write() {
        assertThrowsUOE(() -> INSTANCE.write(0, new byte[1], 0, 0));
    }

    @Test
    void write2() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertThrowsUOE(() -> INSTANCE.write(0, bytes, 0, 0));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    void write3() {
        assertThrowsUOE(() -> INSTANCE.write(0, new byte[1]));
    }

    @Test
    void write4() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertThrowsUOE(() -> INSTANCE.write(0, bytes));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    void readByte() {
        read(EmptyByteStore::readByte);
    }

    @Test
    void peekUnsignedByte() {
        assertEquals(-1, INSTANCE.peekUnsignedByte(0));
    }

    @Test
    void readShort() {
        read(EmptyByteStore::readShort);
    }

    @Test
    void readInt() {
        read(EmptyByteStore::readLong);
    }

    @Test
    void readLong() {
        read(EmptyByteStore::readLong);
    }

    @Test
    void readFloat() {
        read(EmptyByteStore::readFloat);
    }

    @Test
    void readDouble() {
        read(EmptyByteStore::readDouble);
    }

    @Test
    void readVolatileByte() {
        read(EmptyByteStore::readVolatileByte);
    }

    @Test
    void readVolatileShort() {
        read(EmptyByteStore::readVolatileShort);
    }

    @Test
    void readVolatileInt() {
        read(EmptyByteStore::readVolatileInt);
    }

    @Test
    void readVolatileLong() {
        read(EmptyByteStore::readVolatileLong);
    }

    @Test
    void isDirectMemory() {
        assertFalse(INSTANCE.isDirectMemory());
    }

    @Test
    void hashCodeTest() {
        int actual = INSTANCE.hashCode();
        int expected = NativeBytesStore.from("").hashCode();
        assertEquals(expected, actual);
    }

    @Test
    void equalsTest() {
        assertNotEquals(null, INSTANCE);
        assertEquals(NativeBytesStore.from(""), INSTANCE);
    }

    @Test
    void copy() {
        final BytesStore<EmptyByteStore, Void> copy = INSTANCE.copy();
        assertEquals(INSTANCE, copy);
    }

    @Test
    void bytesForRead() {
        final Bytes<Void> bytes = INSTANCE.bytesForRead();
        try {
            assertEquals(0, bytes.capacity());
            assertEquals(0, bytes.readPosition());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    void capacity() {
        assertEquals(0, INSTANCE.capacity());
    }

    @Test
    void underlyingObject() {
        assertNull(INSTANCE.underlyingObject());
    }

    @Test
    void inside() {
        assertFalse(INSTANCE.inside(0, 0));
        assertFalse(INSTANCE.inside(0, 1));
        assertFalse(INSTANCE.inside(1, 0));
    }

    @Test
    void testInside() {
        assertFalse(INSTANCE.inside(0));
    }

    @Test
    void copyTo() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertDoesNotThrow(() -> INSTANCE.copyTo(bytes));
        } finally {
            bytes.releaseLast();
        }

        final byte[] arr = new byte[1];
        arr[0] = 13;
        assertDoesNotThrow(() -> INSTANCE.copyTo(arr));
        assertEquals(13, arr[0]);

        final ByteBuffer bb = ByteBuffer.allocate(1);
        assertDoesNotThrow(() -> INSTANCE.copyTo(bb));
        assertEquals(0, bb.position());

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> INSTANCE.copyTo(os));
        final byte[] toByteArray = os.toByteArray();
        assertEquals(0, toByteArray.length);
    }

    @Test
    void nativeWrite() {
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.nativeWrite(34, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.nativeWrite(34, 0, -1));
        assertThrowsUOE(() -> INSTANCE.nativeWrite(34, 0, 0));
    }

    @Test
    void write8bit() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        try {
            assertThrows(BufferOverflowException.class, () -> INSTANCE.write8bit(0, bs));
            assertThrows(IllegalArgumentException.class, () -> INSTANCE.write8bit(-1, bs));
        } finally {
            bs.releaseLast();
        }
    }

    @Test
    void testWrite8bit() {
        assertThrows(BufferOverflowException.class, () -> INSTANCE.write8bit(0, "A", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.write8bit(-1, "A", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.write8bit(-1, "A", 0, -1));
    }

    @Test
    void nativeRead() {
        assertThrowsUOE(() -> INSTANCE.nativeRead(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.nativeRead(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.nativeRead(0, 1, -1));
    }

    @Test
    void compareAndSwapInt() {
        assertThrowsUOE(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapInt(0, 1, 1));
    }

    @Test
    void compareAndSwapLong() {
        assertThrowsUOE(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapLong(0, 1L, 1L));
    }

    @Test
    void compareAndSwapDouble() {
        assertThrowsUOE(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapDouble(0, 1d, 1d));
    }

    @Test
    void compareAndSwapFloat() {
        assertThrowsUOE(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapFloat(0, 1f, 1f));
    }

    @Test
    void testAndSetInt() {
        assertThrowsUOE(() -> ((RandomDataOutput<?>) INSTANCE).testAndSetInt(0, 1, 1));
    }

    @Test
    void equalBytes() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        final BytesStore<?, ?> emptyBs = BytesStore.from("");
        try {
            assertTrue(INSTANCE.equalBytes(bs, 0));
            assertTrue(INSTANCE.equalBytes(emptyBs, 1));
            assertThrows(IllegalArgumentException.class, () -> INSTANCE.equalBytes(bs, -1));
        } finally {
            bs.releaseLast();
            emptyBs.releaseLast();
        }
    }

    @Test
    void move() {
        assertThrowsUOE(() -> INSTANCE.move(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.move(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.move(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.move(0, 0, -1));
    }

    @Test
    void addressForRead() {
        assertThrowsUOE(() -> INSTANCE.addressForRead(0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.addressForRead(-1));
    }

    @Test
    void addressForWrite() {
        assertThrowsUOE(() -> INSTANCE.addressForWrite(0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.addressForWrite(-1));
    }

    @Test
    void addressForWritePosition() {
        assertThrowsUOE(INSTANCE::addressForWritePosition);
    }

    @Test
    void bytesForWrite() {
        assertThrowsUOE(INSTANCE::bytesForWrite);
    }

    @Test
    void sharedMemory() {
        assertFalse(INSTANCE.sharedMemory());
    }

    @Test
    void isImmutableEmptyByteStore() {
        assertTrue(INSTANCE.isImmutableEmptyByteStore());
    }

    @Test
    void testToString() {
        final BytesStore<?, ?> bytes = Bytes.from("");
        final BytesStore<?, ?> bs = bytes.bytesStore();
        assertNotNull(bs);
        try {
            assertEquals(bs.toString(), INSTANCE.toString());
            assertEquals(bs.toDebugString(), INSTANCE.toDebugString());
            assertEquals(bs.toDebugString(2), INSTANCE.toDebugString(2));
            assertEquals(bs.to8bitString(), INSTANCE.to8bitString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    void chars() {
        assertEquals(0, INSTANCE.chars().count());
    }

    @Test
    void codePoints() {
        assertEquals(0, INSTANCE.codePoints().count());
    }

    @Test
    void length() {
        assertEquals(0, INSTANCE.length());
    }

    @Test
    void charAt() {
        assertThrows(IndexOutOfBoundsException.class, () -> INSTANCE.charAt(-1));
    }

    @Test
    void subSequence() {
        assertThrows(IndexOutOfBoundsException.class, () -> INSTANCE.subSequence(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> INSTANCE.subSequence(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> INSTANCE.subSequence(1, 2));
    }

    @Test
    void zeroOut() {
        assertDoesNotThrow(() -> INSTANCE.zeroOut(0, 0));
        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(0, 1));
        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(1, 2));
    }

    void read(final ObjLongConsumer<EmptyByteStore> getter) {
        assertThrowsUOE(() -> getter.accept(INSTANCE, 0));
        assertThrows(IllegalArgumentException.class, () -> getter.accept(INSTANCE, -1));
    }

    void assertThrowsUOE(final Runnable consumer) {
        assertThrows(UnsupportedOperationException.class, consumer::run);
    }

}