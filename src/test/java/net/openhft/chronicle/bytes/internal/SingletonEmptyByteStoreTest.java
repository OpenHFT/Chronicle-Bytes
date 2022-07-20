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
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.ObjLongConsumer;

import static net.openhft.chronicle.bytes.Bytes.elasticByteBuffer;
import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.jupiter.api.Assertions.*;

class EmptyBytesTest {

    private static final Bytes INSTANCE = Bytes.empty();

    @Test
    void refCount() {
        assertNotEquals(0, INSTANCE.refCount());
    }

    @Test
    void writeByteInt() {
        assertThrowsBufferException(() -> INSTANCE.writeByte(0, 0));
    }

    @Test
    void writeByte() {
        assertThrowsBufferException(() -> INSTANCE.writeByte(0, (byte) 0));
    }

    @Test
    void writeShort() {
        assertThrowsBufferException(() -> INSTANCE.writeShort(0, (short) 0));
    }

    @Test
    void writeInt() {
        assertThrowsBufferException(() -> INSTANCE.writeInt(0, 0));
    }

    @Test
    void writeOrderedInt() {
        assertThrowsBufferException(() -> INSTANCE.writeOrderedInt(0, 0));
    }

    @Test
    void writeLong() {
        assertThrowsBufferException(() -> INSTANCE.writeLong(0, 0));
    }

    @Test
    void writeOrderedLong() {
        assertThrowsBufferException(() -> INSTANCE.writeOrderedLong(0, 0L));
    }

    @Test
    void writeFloat() {
        assertThrowsBufferException(() -> INSTANCE.writeFloat(0, 0.0f));
    }

    @Test
    void writeDouble() {
        assertThrowsBufferException(() -> INSTANCE.writeDouble(0, 0.0d));
    }

    @Test
    void writeVolatileByte() {
        assertThrowsBufferException(() -> INSTANCE.writeVolatileByte(0, (byte) 0));
    }

    @Test
    void writeVolatileShort() {
        assertThrowsBufferException(() -> INSTANCE.writeVolatileShort(0, (short) 0));
    }

    @Test
    void writeVolatileInt() {
        assertThrowsBufferException(() -> INSTANCE.writeVolatileInt(0, 0));
    }

    @Test
    void writeVolatileLong() {
        assertThrowsBufferException(() -> INSTANCE.writeVolatileLong(0, 0L));
    }

    @Test
    void write() {
        assertThrowsBufferException(() -> INSTANCE.write(0, new byte[1], 0, 0));
    }

    @Test
    void write2() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertThrowsBufferException(() -> INSTANCE.write(0, bytes, 0, 0));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    void write3() {
        assertThrowsBufferException(() -> INSTANCE.write(0, new byte[1]));
    }

    @Test
    void write4() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertThrowsBufferException(() -> INSTANCE.write(0, bytes));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    void readByte() {
        read(BytesStore::readByte);
    }

    @Test
    void peekUnsignedByte() {
        assertEquals(-1, INSTANCE.peekUnsignedByte(0));
    }

    @Test
    void readShort() {
        read(BytesStore::readShort);
    }

    @Test
    void readInt() {
        read(BytesStore::readLong);
    }

    @Test
    void readLong() {
        read(BytesStore::readLong);
    }

    @Test
    void readFloat() {
        read(BytesStore::readFloat);
    }

    @Test
    void readDouble() {
        read(BytesStore::readDouble);
    }

    @Test
    void readVolatileByte() {
        read(BytesStore::readVolatileByte);
    }

    @Test
    void readVolatileShort() {
        read(BytesStore::readVolatileShort);
    }

    @Test
    void readVolatileInt() {
        read(BytesStore::readVolatileInt);
    }

    @Test
    void readVolatileLong() {
        read(BytesStore::readVolatileLong);
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
        final BytesStore<?, Void> copy = INSTANCE.copy();
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
        assertThrowsBufferException(() -> INSTANCE.nativeWrite(34, 0, 0));
    }

    @Test
    void write8bit() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        try {
            assertThrows(BufferOverflowException.class, () -> INSTANCE.write8bit(0, bs));
            assertThrows(BufferUnderflowException.class, () -> INSTANCE.write8bit(-1, bs));
        } finally {
            bs.releaseLast();
        }
    }

    @Test
    void testWrite8bit() {
        assertThrowsBufferException(() -> INSTANCE.write8bit(0, "A", 0, 1));
        assertThrowsBufferException(() -> INSTANCE.write8bit(-1, "A", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.write8bit(0, "A", 0, -1));
    }

    @Test
    void nativeRead() {
        assertThrowsBufferException(() -> INSTANCE.nativeRead(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.nativeRead(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.nativeRead(0, 1, -1));
    }

    @Test
    void compareAndSwapInt() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapInt(0, 1, 1));
    }

    @Test
    void compareAndSwapLong() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapLong(0, 1L, 1L));
    }

    @Test
    void compareAndSwapDouble() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapDouble(0, 1d, 1d));
    }

    @Test
    void compareAndSwapFloat() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) INSTANCE).compareAndSwapFloat(0, 1f, 1f));
    }

    @Test
    void testAndSetInt() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) INSTANCE).testAndSetInt(0, 1, 1));
    }

    @Test
    void equalBytes() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        final BytesStore<?, ?> emptyBs = BytesStore.from("");
        try {
            assertTrue(INSTANCE.equalBytes(bs, 0));
            assertFalse(INSTANCE.equalBytes(emptyBs, 1));
            assertTrue(INSTANCE.equalBytes(emptyBs, 0));
            assertTrue(INSTANCE.equalBytes(bs, -1));
        } finally {
            bs.releaseLast();
            emptyBs.releaseLast();
        }
    }

    @Test
    void move() {
        assertThrowsBufferException(() -> INSTANCE.move(0, 0, 0));
        assertThrowsBufferException(() -> INSTANCE.move(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.move(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> INSTANCE.move(0, 0, -1));
    }

    @Test
    void addressForRead() {
        assertThrowsBufferException(() -> INSTANCE.addressForRead(0));
        assertThrowsBufferException(() -> INSTANCE.addressForRead(-1));
    }

    @Test
    void addressForWrite() {
        assertThrowsBufferException(() -> INSTANCE.addressForWrite(0));
        assertThrowsBufferException(() -> INSTANCE.addressForWrite(-1));
    }

    @Test
    void addressForWritePosition() {
        assertThrowsUOE(INSTANCE::addressForWritePosition);
    }

    @Test
    void bytesForWrite() {
        assertThrowsBufferException(() -> INSTANCE.bytesForWrite().writeSkip(1));
    }

    @Test
    void sharedMemory() {
        assertFalse(INSTANCE.sharedMemory());
    }

    @Test
    void isImmutableBytesStore() {
        assertEquals(0, INSTANCE.capacity());
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
        // outside bounds are ignored
//        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(0, 1));
//        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(1, 2));
    }

    void read(final ObjLongConsumer<BytesStore> getter) {
        assertThrowsBufferException(() -> getter.accept(INSTANCE, 0));
        assertThrows(DecoratedBufferUnderflowException.class, () -> getter.accept(INSTANCE, -1));
    }

    void assertThrowsUOE(final Runnable consumer) {
        assertThrows(UnsupportedOperationException.class, consumer::run);
    }

    void assertThrowsBufferException(final Runnable consumer) {
        try {
            consumer.run();
        } catch (BufferOverflowException | BufferUnderflowException e) {
            // expected
        } catch (Throwable t) {
            throw new AssertionFailedError("expected Buffer*Exception", t);
        }
    }

}