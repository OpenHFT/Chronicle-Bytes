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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.RandomDataOutput;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.After;
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
import static org.junit.Assume.assumeFalse;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(Parameterized.class)
public class EmptyBytesStoreTest extends BytesTestCommon {

    private final BytesStore instance;

    public EmptyBytesStoreTest(String type, BytesStore instance) {
        this.instance = instance;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Bytes.empty()", Bytes.empty()},
                {"BytesStore.empty()", BytesStore.empty()},
                {"native", NativeBytesStore.nativeStoreWithFixedCapacity(0)},
                {"NativeByteStore.bytesForRead()", NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForRead()},
                {"NativeByteStore.bytesForWrite()", NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForWrite()},
        });
    }

    @After
    public void teardown() {
        IOTools.unmonitor(instance);
    }

    @Test
    public void notSameAsEmpty() {
        // a case which should produce a different instance. Wire depends on this
        assertNotSame(BytesStore.wrap(new byte[0]), instance);
    }

    @Test
    public void refCount() {
        assertNotEquals(0, instance.refCount());
    }

    @Test
    public void writeByteInt() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeByte(0, 0));
    }

    @Test
    public void writeByte() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeByte(0, (byte) 0));
    }

    @Test
    public void writeShort() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeShort(0, (short) 0));
    }

    @Test
    public void writeInt() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeInt(0, 0));
    }

    @Test
    public void writeOrderedInt() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeOrderedInt(0, 0));
    }

    @Test
    public void writeLong() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeLong(0, 0));
    }

    @Test
    public void writeOrderedLong() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeOrderedLong(0, 0L));
    }

    @Test
    public void writeFloat() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeFloat(0, 0.0f));
    }

    @Test
    public void writeDouble() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeDouble(0, 0.0d));
    }

    @Test
    public void writeVolatileByte() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileByte(0, (byte) 0));
    }

    @Test
    public void writeVolatileShort() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileShort(0, (short) 0));
    }

    @Test
    public void writeVolatileInt() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileInt(0, 0));
    }

    @Test
    public void writeVolatileLong() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileLong(0, 0L));
    }

    @Test
    public void write() {
        assertDoesNotThrow(() -> instance.write(0, new byte[1], 0, 0));
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.write(0, new byte[1], 0, 1));
    }

    @Test
    public void write2() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        bytes.append("Hello");
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes, 0, 0));
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, bytes, 0, 1));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void write3() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.write(0, new byte[1]));
    }

    @Test
    public void write4() {
        final Bytes<ByteBuffer> bytes = elasticByteBuffer();
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes));
            bytes.append("Hello");
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, bytes));
            assertThrowsBufferException(() -> instance.write(1, bytes));
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
        assertNotEquals(instance, null);
        assertEquals(NativeBytesStore.from(""), instance);
        assertEquals(instance, NativeBytesStore.from(""));
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
        assertDoesNotThrow(() -> instance.nativeWrite(34, 0, 0));
    }

    @Test
    public void write8bit() {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(-1, bs));
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrows(BufferOverflowException.class, () -> instance.write8bit(0, bs));
    }

    @Test
    public void testWrite8bit() {
        assertThrowsBufferException(() -> instance.write8bit(0, "A", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(-1, "A", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(0, "A", 0, -1));
    }

    @Test
    public void nativeRead() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.nativeRead(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeRead(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeRead(0, 1, -1));
    }

    @Test
    public void compareAndSwapInt() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapInt(0, 1, 1));
    }

    @Test
    public void compareAndSwapLong() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapLong(0, 1L, 1L));
    }

    @Test
    public void compareAndSwapDouble() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapDouble(0, 1d, 1d));
    }

    @Test
    public void compareAndSwapFloat() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapFloat(0, 1f, 1f));
    }

    @Test
    public void testAndSetInt() {
        assumeFalse(instance instanceof NativeBytesStore);
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
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrows(IllegalArgumentException.class, () -> instance.equalBytes(bs, -1));
        } finally {
            bs.releaseLast();
            emptyBs.releaseLast();
        }
    }

    @Test
    public void move() {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.move(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.move(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.move(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.move(0, 0, -1));
    }

    @Test
    public void addressForRead() {
        assertThrowsBufferException(() -> instance.addressForRead(1));
        assertThrows(IllegalArgumentException.class, () -> instance.addressForRead(-1));
        assumeFalse(instance.isDirectMemory());
        assertThrowsBufferException(() -> instance.addressForRead(0));
    }

    @Test
    public void addressForWrite() {
        assertThrowsBufferException(() -> instance.addressForWrite(1));
        assertThrows(IllegalArgumentException.class, () -> instance.addressForWrite(-1));
        assumeFalse(instance.isDirectMemory());
        assertThrowsBufferException(() -> instance.addressForWrite(0));
    }

    @Test
    public void addressForWritePosition() {
        assumeFalse(instance instanceof NativeBytesStore);
        assumeFalse(instance.bytesStore() instanceof NativeBytesStore);
        assertThrowsBufferException(instance::addressForWritePosition);
    }

    @Test
    public void bytesForWrite() {
        try {
            final Bytes bytes = instance.bytesForWrite();
            IOTools.unmonitor(bytes);
            assertThrowsBufferException(() -> bytes.writeSkip(1));
        } catch (UnsupportedOperationException ignored) {

        }
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
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrows(IllegalArgumentException.class, () -> instance.charAt(-1));
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
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> getter.accept(instance, 0));
        assertThrows(IllegalArgumentException.class, () -> getter.accept(instance, -1));
    }

    public void assertThrows(Class<? extends Throwable> tClass, Runnable runnable) {
        try {
            runnable.run();

        } catch (UnsupportedOperationException ignored) {
            return;
        } catch (Throwable t) {
            if (tClass.isInstance(t))
                return;
            throw new AssertionFailedError("Unexpected exception type thrown", tClass, t.getClass(), t);
        }
        throw new AssertionFailedError("expected " + tClass);
    }

    public void assertThrowsBufferException(final Runnable consumer) {
        try {
            consumer.run();

        } catch (BufferOverflowException | BufferUnderflowException | UnsupportedOperationException e) {
            return;
        } catch (Throwable t) {
            throw new AssertionFailedError("expected Buffer*Exception", t);
        }
        throw new AssertionFailedError("expected Buffer*Exception");
    }

}