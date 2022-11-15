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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.ObjLongConsumer;

import static net.openhft.chronicle.bytes.Bytes.elasticByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(Parameterized.class)
public class EmptyBytesTest extends BytesTestCommon {

    private final BytesStore instance;

    public EmptyBytesTest(String type, BytesStore instance) {
        this.instance = instance;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"default", Bytes.empty()},
                {"heap-read", HeapBytesStore.wrap((byte[]) null).bytesForRead()},
                {"heap-write", HeapBytesStore.wrap((byte[]) null).bytesForWrite()},
        });
    }

    @Test
    public void refCount() {
        assertNotEquals(0, instance.refCount());
    }

    @Test
    public void writeByteInt() {
        assertThrowsBufferException(() -> instance.writeByte(0, 0));
    }

    @Test
    public void writeByte() {
        assertThrowsBufferException(() -> instance.writeByte(0, (byte) 0));
    }

    @Test
    public void writeShort() {
        assertThrowsBufferException(() -> instance.writeShort(0, (short) 0));
    }

    @Test
    public void writeInt() {
        assertThrowsBufferException(() -> instance.writeInt(0, 0));
    }

    @Test
    public void writeOrderedInt() {
        assertThrowsBufferException(() -> instance.writeOrderedInt(0, 0));
    }

    @Test
    public void writeLong() {
        assertThrowsBufferException(() -> instance.writeLong(0, 0));
    }

    @Test
    public void writeOrderedLong() {
        assertThrowsBufferException(() -> instance.writeOrderedLong(0, 0L));
    }

    @Test
    public void writeFloat() {
        assertThrowsBufferException(() -> instance.writeFloat(0, 0.0f));
    }

    @Test
    public void writeDouble() {
        assertThrowsBufferException(() -> instance.writeDouble(0, 0.0d));
    }

    @Test
    public void writeVolatileByte() {
        assertThrowsBufferException(() -> instance.writeVolatileByte(0, (byte) 0));
    }

    @Test
    public void writeVolatileShort() {
        assertThrowsBufferException(() -> instance.writeVolatileShort(0, (short) 0));
    }

    @Test
    public void writeVolatileInt() {
        assertThrowsBufferException(() -> instance.writeVolatileInt(0, 0));
    }

    @Test
    public void writeVolatileLong() {
        assertThrowsBufferException(() -> instance.writeVolatileLong(0, 0L));
    }

    @Test
    public void write() {
        assertThrowsBufferException(() -> instance.write(0, new byte[1], 0, 0));
    }

    @Test
    public void write2() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertThrowsBufferException(() -> instance.write(0, bytes, 0, 0));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void write3() {
        assertThrowsBufferException(() -> instance.write(0, new byte[1]));
    }

    @Test
    public void write4() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertThrowsBufferException(() -> instance.write(0, bytes));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void readByte() {
        read(BytesStore::readByte);
    }

    @Test
    public void peekUnsignedByte() {
        assertEquals(-1, instance.peekUnsignedByte(0));
    }

    @Test
    public void readShort() {
        read(BytesStore::readShort);
    }

    @Test
    public void readInt() {
        read(BytesStore::readLong);
    }

    @Test
    public void readLong() {
        read(BytesStore::readLong);
    }

    @Test
    public void readFloat() {
        read(BytesStore::readFloat);
    }

    @Test
    public void readDouble() {
        read(BytesStore::readDouble);
    }

    @Test
    public void readVolatileByte() {
        read(BytesStore::readVolatileByte);
    }

    @Test
    public void readVolatileShort() {
        read(BytesStore::readVolatileShort);
    }

    @Test
    public void readVolatileInt() {
        read(BytesStore::readVolatileInt);
    }

    @Test
    public void readVolatileLong() {
        read(BytesStore::readVolatileLong);
    }

    @Test
    public void hashCodeTest() {
        int actual = instance.hashCode();
        int expected = NativeBytesStore.from("").hashCode();
        assertEquals(expected, actual);
    }

    @Test
    public void equalsTest() {
        assertNotEquals(null, instance);
        assertEquals(NativeBytesStore.from(""), instance);
    }

    @Test
    public void copy() {
        final BytesStore<?, Void> copy = instance.copy();
        assertEquals(instance, copy);
    }

    @Test
    public void bytesForRead() {
        final Bytes<Void> bytes = instance.bytesForRead();
        try {
            assertEquals(0, bytes.capacity());
            assertEquals(0, bytes.readPosition());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void capacity() {
        assertEquals(0, instance.capacity());
    }

    @Test
    public void underlyingObject() {
        assertNull(instance.underlyingObject());
    }

    @Test
    public void inside() {
        assertFalse(instance.inside(0, 0));
        assertFalse(instance.inside(0, 1));
        assertFalse(instance.inside(1, 0));
    }

    @Test
    public void testInside() {
        assertFalse(instance.inside(0));
    }

    @Test
    public void copyTo() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertDoesNotThrow(() -> instance.copyTo(bytes));
        } finally {
            bytes.releaseLast();
        }

        final byte[] arr = new byte[1];
        arr[0] = 13;
        assertDoesNotThrow(() -> instance.copyTo(arr));
        assertEquals(13, arr[0]);

        final ByteBuffer bb = ByteBuffer.allocate(1);
        assertDoesNotThrow(() -> instance.copyTo(bb));
        assertEquals(0, bb.position());

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> instance.copyTo(os));
        final byte[] toByteArray = os.toByteArray();
        assertEquals(0, toByteArray.length);
    }

    @Test
    public void nativeWrite() {
        assertThrows(IllegalArgumentException.class, () -> instance.nativeWrite(34, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeWrite(34, 0, -1));
        assertThrowsBufferException(() -> instance.nativeWrite(34, 0, 0));
    }

    @Test
    public void write8bit() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        assertThrows(BufferOverflowException.class, () -> instance.write8bit(0, bs));
        assertThrows(BufferUnderflowException.class, () -> instance.write8bit(-1, bs));
    }

    @Test
    public void testWrite8bit() {
        assertThrowsBufferException(() -> instance.write8bit(0, "A", 0, 1));
        assertThrowsBufferException(() -> instance.write8bit(-1, "A", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(0, "A", 0, -1));
    }

    @Test
    public void nativeRead() {
        assertThrowsBufferException(() -> instance.nativeRead(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeRead(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeRead(0, 1, -1));
    }

    @Test
    public void compareAndSwapInt() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapInt(0, 1, 1));
    }

    @Test
    public void compareAndSwapLong() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapLong(0, 1L, 1L));
    }

    @Test
    public void compareAndSwapDouble() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapDouble(0, 1d, 1d));
    }

    @Test
    public void compareAndSwapFloat() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapFloat(0, 1f, 1f));
    }

    @Test
    public void testAndSetInt() {
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).testAndSetInt(0, 1, 1));
    }

    @Test
    public void equalBytes() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        final BytesStore<?, ?> emptyBs = BytesStore.from("");
        try {
            assertTrue(instance.equalBytes(bs, 0));
            assertFalse(instance.equalBytes(emptyBs, 1));
            assertTrue(instance.equalBytes(emptyBs, 0));
            assertTrue(instance.equalBytes(bs, -1));
        } finally {
            bs.releaseLast();
            emptyBs.releaseLast();
        }
    }

    @Test
    public void move() {
        assertThrowsBufferException(() -> instance.move(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.move(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.move(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.move(0, 0, -1));
    }

    @Test
    public void addressForRead() {
        assertThrowsBufferException(() -> instance.addressForRead(0));
        assertThrowsBufferException(() -> instance.addressForRead(-1));
    }

    @Test
    public void addressForWrite() {
        assertThrowsBufferException(() -> instance.addressForWrite(0));
        assertThrowsBufferException(() -> instance.addressForWrite(-1));
    }

    @Test
    public void addressForWritePosition() {
        assertThrowsBufferException(instance::addressForWritePosition);
    }

    @Test
    public void bytesForWrite() {
        assertThrowsBufferException(() -> instance.bytesForWrite().writeSkip(1));
    }

    @Test
    public void sharedMemory() {
        assertFalse(instance.sharedMemory());
    }

    @Test
    public void isImmutableBytesStore() {
        assertEquals(0, instance.capacity());
    }

    @Test
    public void testToString() {
        final BytesStore<?, ?> bytes = Bytes.from("");
        final BytesStore<?, ?> bs = bytes.bytesStore();
        assertNotNull(bs);
        try {
            assertEquals(bs.toString(), instance.toString());
            assertEquals(bs.toDebugString(), instance.toDebugString());
            assertEquals(bs.toDebugString(2), instance.toDebugString(2));
            assertEquals(bs.to8bitString(), instance.to8bitString());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void chars() {
        assertEquals(0, instance.chars().count());
    }

    @Test
    public void codePoints() {
        assertEquals(0, instance.codePoints().count());
    }

    @Test
    public void length() {
        assertEquals(0, instance.length());
    }

    @Test
    public void charAt() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.charAt(-1));
    }

    @Test
    public void subSequence() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subSequence(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subSequence(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subSequence(1, 2));
    }

    @Test
    public void zeroOut() {
        assertDoesNotThrow(() -> instance.zeroOut(0, 0));
        // outside bounds are ignored
//        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(0, 1));
//        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(1, 2));
    }

    public void read(final ObjLongConsumer<BytesStore> getter) {
        assertThrowsBufferException(() -> getter.accept(instance, 0));
        assertThrows(DecoratedBufferUnderflowException.class, () -> getter.accept(instance, -1));
    }

    public void assertThrowsBufferException(final Runnable consumer) {
        try {
            consumer.run();
        } catch (BufferOverflowException | BufferUnderflowException e) {
            // expected
        } catch (Throwable t) {
            throw new AssertionFailedError("expected Buffer*Exception", t);
        }
    }

}