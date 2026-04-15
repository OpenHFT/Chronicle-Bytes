/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.RandomDataOutput;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.ObjLongConsumer;

import static net.openhft.chronicle.bytes.Bytes.elasticHeapByteBuffer;
import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.*;

public class EmptyBytesStoreTest extends BytesTestCommon {

    private BytesStore<?, ?> instance;

    public void initEmptyBytesStoreTest(String type, BytesStore<?, ?> instance) {
        this.instance = instance;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Bytes.empty()", Bytes.empty()},
                {"BytesStore.empty()", BytesStore.empty()},
                {"native", NativeBytesStore.nativeStoreWithFixedCapacity(0)},
                {"NativeByteStore.bytesForRead()", NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForRead()},
                {"NativeByteStore.bytesForWrite()", NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForWrite()},
        });
    }

    @AfterEach
    public void teardown() {
        IOTools.unmonitor(instance);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void notSameAsEmpty(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        // a case which should produce a different instance. Wire depends on this
        assertNotSame(BytesStore.wrap(new byte[0]), instance);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void refCount(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertNotEquals(0, instance.refCount());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeByteInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeByte(0, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeByte(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeByte(0, (byte) 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeShort(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeShort(0, (short) 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeInt(0, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeOrderedInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeOrderedInt(0, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeLong(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeLong(0, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeOrderedLong(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeOrderedLong(0, 0L));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeFloat(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeFloat(0, 0.0f));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeDouble(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeDouble(0, 0.0d));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeVolatileByte(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileByte(0, (byte) 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeVolatileShort(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileShort(0, (short) 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeVolatileInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileInt(0, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void writeVolatileLong(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.writeVolatileLong(0, 0L));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertDoesNotThrow(() -> instance.write(0, new byte[1], 0, 0));
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.write(0, new byte[1], 0, 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write2(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
        bytes.append("Hello");
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes, 0, 0));
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, bytes, 0, 1));
        } finally {
            bytes.releaseLast();
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write3(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.write(0, new byte[1]));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write4(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
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

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readByte(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readByte);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void peekUnsignedByte(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertEquals(-1, instance.peekUnsignedByte(0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readShort(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readShort);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readLong);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readLong(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readLong);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readFloat(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readFloat);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readDouble(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readDouble);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readVolatileByte(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readVolatileByte);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readVolatileShort(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readVolatileShort);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readVolatileInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readVolatileInt);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void readVolatileLong(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        read(BytesStore::readVolatileLong);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void hashCodeTest(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        int actual = instance.hashCode();
        int expected = NativeBytesStore.from("").hashCode();
        assertEquals(expected, actual);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void equalsTest(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertNotEquals(null, instance);
        assertNotEquals(instance, null);
        assertEquals(NativeBytesStore.from(""), instance);
        assertEquals(instance, NativeBytesStore.from(""));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void copy(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        final BytesStore<?, Void> copy = uncheckedCast(instance.copy());
        assertEquals(instance, copy);
        copy.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void bytesForRead(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        final Bytes<Void> bytes = uncheckedCast(instance.bytesForRead());
        try {
            assertEquals(0, bytes.capacity());
            assertEquals(0, bytes.readPosition());
        } finally {
            bytes.releaseLast();
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void capacity(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertEquals(0, instance.capacity());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void underlyingObject(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertNull(instance.underlyingObject());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void inside(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertTrue(instance.inside(0, 0));  // Nothing at index zero is in the empty store
        assertFalse(instance.inside(0, 1));
        assertFalse(instance.inside(1, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testInside(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertFalse(instance.inside(0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void copyTo(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
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

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void nativeWrite(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertThrows(IllegalArgumentException.class, () -> instance.nativeWrite(34, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeWrite(34, 0, -1));
        assertDoesNotThrow(() -> instance.nativeWrite(34, 0, 0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void write8bit(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        final BytesStore<?, ?> bs = BytesStore.from("A");
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(-1, bs));
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrows(BufferOverflowException.class, () -> instance.write8bit(0, bs));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testWrite8bit(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertThrowsBufferException(() -> instance.write8bit(0, "A", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(-1, "A", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.write8bit(0, "A", 0, -1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void nativeRead(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.nativeRead(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeRead(-1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.nativeRead(0, 1, -1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void compareAndSwapInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapInt(0, 1, 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void compareAndSwapLong(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapLong(0, 1L, 1L));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void compareAndSwapDouble(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapDouble(0, 1d, 1d));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void compareAndSwapFloat(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapFloat(0, 1f, 1f));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAndSetInt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).testAndSetInt(0, 1, 1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void equalBytes(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
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

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void move(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> instance.move(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> instance.move(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.move(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> instance.move(0, 0, -1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void addressForRead(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertThrowsBufferException(() -> instance.addressForRead(1));
        assertThrows(IllegalArgumentException.class, () -> instance.addressForRead(-1));
        assumeFalse(instance.isDirectMemory());
        assertThrowsBufferException(() -> instance.addressForRead(0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void addressForWrite(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertThrowsBufferException(() -> instance.addressForWrite(1));
        assertThrows(IllegalArgumentException.class, () -> instance.addressForWrite(-1));
        assumeFalse(instance.isDirectMemory());
        assertThrowsBufferException(() -> instance.addressForWrite(0));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void addressForWritePosition(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assumeFalse(instance.bytesStore() instanceof NativeBytesStore);
        assertThrowsBufferException(instance::addressForWritePosition);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void bytesForWrite(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        try {
            final Bytes<?> bytes = instance.bytesForWrite();
            IOTools.unmonitor(bytes);
            assertThrowsBufferException(() -> bytes.writeSkip(1));
        } catch (UnsupportedOperationException ignored) {

        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void sharedMemory(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertFalse(instance.sharedMemory());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void isImmutableBytesStore(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertEquals(0, instance.capacity());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testToString(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
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

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void chars(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertEquals(0, instance.chars().count());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void codePoints(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertEquals(0, instance.codePoints().count());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void length(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertEquals(0, instance.length());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void charAt(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrows(IllegalArgumentException.class, () -> instance.charAt(-1));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void subSequence(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subSequence(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subSequence(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subSequence(1, 2));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void zeroOut(String type, BytesStore<?, ?> instance) {
        initEmptyBytesStoreTest(type, instance);
        assertDoesNotThrow(() -> instance.zeroOut(0, 0));
        // outside bounds are ignored
//        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(0, 1));
//        assertThrows(BufferOverflowException.class, () -> INSTANCE.zeroOut(1, 2));
    }

    private void read(final ObjLongConsumer<BytesStore<?, ?>> getter) {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> getter.accept(instance, 0));
        assertThrows(IllegalArgumentException.class, () -> getter.accept(instance, -1));
    }

    private void assertThrows(Class<? extends Throwable> tClass, Runnable runnable) {
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

    private void assertThrowsBufferException(final Runnable consumer) {
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
