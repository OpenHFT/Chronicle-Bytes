/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.RandomDataOutput;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.ObjLongConsumer;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.Bytes.elasticHeapByteBuffer;
import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class EmptyBytesStoreTest extends BytesTestCommon {

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("Bytes.empty()", Bytes.empty()),
                Arguments.of("BytesStore.empty()", BytesStore.empty()),
                Arguments.of("native", NativeBytesStore.nativeStoreWithFixedCapacity(0)),
                Arguments.of("NativeByteStore.bytesForRead()", NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForRead()),
                Arguments.of("NativeByteStore.bytesForWrite()", NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForWrite())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void notSameAsEmpty(String type, BytesStore<?, ?> instance) {
        try {
            // a case which should produce a different instance. Wire depends on this
            assertNotSame(BytesStore.wrap(new byte[0]), instance);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void refCount(String type, BytesStore<?, ?> instance) {
        try {
            assertNotEquals(0, instance.refCount());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeByteInt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeByte(0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeByte(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeByte(0, (byte) 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeShort(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeShort(0, (short) 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeInt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeInt(0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeOrderedInt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeOrderedInt(0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeLong(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeLong(0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeOrderedLong(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeOrderedLong(0, 0L));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeFloat(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeFloat(0, 0.0f));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeDouble(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeDouble(0, 0.0d));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeVolatileByte(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeVolatileByte(0, (byte) 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeVolatileShort(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeVolatileShort(0, (short) 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeVolatileInt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeVolatileInt(0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void writeVolatileLong(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.writeVolatileLong(0, 0L));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void write(String type, BytesStore<?, ?> instance) {
        try {
            assertDoesNotThrow(() -> instance.write(0, new byte[1], 0, 0));
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, new byte[1], 0, 1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void write2(String type, BytesStore<?, ?> instance) {
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
        bytes.append("Hello");
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes, 0, 0));
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, bytes, 0, 1));
        } finally {
            bytes.releaseLast();
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void write3(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, new byte[1]));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void write4(String type, BytesStore<?, ?> instance) {
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes));
            bytes.append("Hello");
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.write(0, bytes));
            assertThrowsBufferException(() -> instance.write(1, bytes));
        } finally {
            bytes.releaseLast();
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readByte(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readByte);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void peekUnsignedByte(String type, BytesStore<?, ?> instance) {
        try {
            assertEquals(-1, instance.peekUnsignedByte(0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readShort(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readShort);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readInt(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readLong);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readLong(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readLong);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readFloat(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readFloat);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readDouble(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readDouble);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readVolatileByte(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readVolatileByte);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readVolatileShort(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readVolatileShort);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readVolatileInt(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readVolatileInt);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void readVolatileLong(String type, BytesStore<?, ?> instance) {
        try {
            read(instance, BytesStore::readVolatileLong);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void hashCodeTest(String type, BytesStore<?, ?> instance) {
        try {
            int actual = instance.hashCode();
            int expected = NativeBytesStore.from("").hashCode();
            assertEquals(expected, actual);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void equalsTest(String type, BytesStore<?, ?> instance) {
        try {
            assertNotEquals(null, instance);
            assertNotEquals(instance, null);
            assertEquals(NativeBytesStore.from(""), instance);
            assertEquals(instance, NativeBytesStore.from(""));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void copy(String type, BytesStore<?, ?> instance) {
        try {
            final BytesStore<?, Void> copy = uncheckedCast(instance.copy());
            assertEquals(instance, copy);
            copy.releaseLast();
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void bytesForRead(String type, BytesStore<?, ?> instance) {
        try {
            final Bytes<Void> bytes = uncheckedCast(instance.bytesForRead());
            try {
                assertEquals(0, bytes.capacity());
                assertEquals(0, bytes.readPosition());
            } finally {
                bytes.releaseLast();
            }
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void capacity(String type, BytesStore<?, ?> instance) {
        try {
            assertEquals(0, instance.capacity());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void underlyingObject(String type, BytesStore<?, ?> instance) {
        try {
            assertNull(instance.underlyingObject());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void inside(String type, BytesStore<?, ?> instance) {
        try {
            assertTrue(instance.inside(0, 0));  // Nothing at index zero is in the empty store
            assertFalse(instance.inside(0, 1));
            assertFalse(instance.inside(1, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testInside(String type, BytesStore<?, ?> instance) {
        try {
            assertFalse(instance.inside(0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void copyTo(String type, BytesStore<?, ?> instance) {
        try {
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
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void nativeWrite(String type, BytesStore<?, ?> instance) {
        try {
            assertThrowsAny(IllegalArgumentException.class, () -> instance.nativeWrite(34, -1, 0));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.nativeWrite(34, 0, -1));
            assertDoesNotThrow(() -> instance.nativeWrite(34, 0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void write8bit(String type, BytesStore<?, ?> instance) {
        try {
            final BytesStore<?, ?> bs = BytesStore.from("A");
            assertThrowsAny(IllegalArgumentException.class, () -> instance.write8bit(-1, bs));
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsAny(BufferOverflowException.class, () -> instance.write8bit(0, bs));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testWrite8bit(String type, BytesStore<?, ?> instance) {
        try {
            assertThrowsBufferException(() -> instance.write8bit(0, "A", 0, 1));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.write8bit(-1, "A", -1, 0));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.write8bit(0, "A", 0, -1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void nativeRead(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.nativeRead(0, 1, 1));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.nativeRead(-1, 1, 0));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.nativeRead(0, 1, -1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void compareAndSwapInt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapInt(0, 1, 1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void compareAndSwapLong(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapLong(0, 1L, 1L));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void compareAndSwapDouble(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapDouble(0, 1d, 1d));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void compareAndSwapFloat(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).compareAndSwapFloat(0, 1f, 1f));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testAndSetInt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> ((RandomDataOutput<?>) instance).testAndSetInt(0, 1, 1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void equalBytes(String type, BytesStore<?, ?> instance) {
        final BytesStore<?, ?> bs = BytesStore.from("A");
        final BytesStore<?, ?> emptyBs = BytesStore.from("");
        try {
            assertTrue(instance.equalBytes(bs, 0));
            assertFalse(instance.equalBytes(emptyBs, 1));
            assertTrue(instance.equalBytes(emptyBs, 0));
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsAny(IllegalArgumentException.class, () -> instance.equalBytes(bs, -1));
        } finally {
            bs.releaseLast();
            emptyBs.releaseLast();
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void move(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsBufferException(() -> instance.move(0, 0, 1));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.move(-1, 0, 0));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.move(0, -1, 0));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.move(0, 0, -1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void addressForRead(String type, BytesStore<?, ?> instance) {
        try {
            assertThrowsBufferException(() -> instance.addressForRead(1));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.addressForRead(-1));
            assumeFalse(instance.isDirectMemory());
            assertThrowsBufferException(() -> instance.addressForRead(0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void addressForWrite(String type, BytesStore<?, ?> instance) {
        try {
            assertThrowsBufferException(() -> instance.addressForWrite(1));
            assertThrowsAny(IllegalArgumentException.class, () -> instance.addressForWrite(-1));
            assumeFalse(instance.isDirectMemory());
            assertThrowsBufferException(() -> instance.addressForWrite(0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void addressForWritePosition(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assumeFalse(instance.bytesStore() instanceof NativeBytesStore);
            assertThrowsBufferException(instance::addressForWritePosition);
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void bytesForWrite(String type, BytesStore<?, ?> instance) {
        try {
            try {
                final Bytes<?> bytes = instance.bytesForWrite();
                IOTools.unmonitor(bytes);
                assertThrowsBufferException(() -> bytes.writeSkip(1));
            } catch (UnsupportedOperationException ignored) {

            }
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void sharedMemory(String type, BytesStore<?, ?> instance) {
        try {
            assertFalse(instance.sharedMemory());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void isImmutableBytesStore(String type, BytesStore<?, ?> instance) {
        try {
            assertEquals(0, instance.capacity());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testToString(String type, BytesStore<?, ?> instance) {
        try {
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
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void chars(String type, BytesStore<?, ?> instance) {
        try {
            assertEquals(0, instance.chars().count());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void codePoints(String type, BytesStore<?, ?> instance) {
        try {
            assertEquals(0, instance.codePoints().count());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void length(String type, BytesStore<?, ?> instance) {
        try {
            assertEquals(0, instance.length());
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void charAt(String type, BytesStore<?, ?> instance) {
        try {
            assumeFalse(instance instanceof NativeBytesStore);
            assertThrowsAny(IllegalArgumentException.class, () -> instance.charAt(-1));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void subSequence(String type, BytesStore<?, ?> instance) {
        try {
            assertThrowsAny(IndexOutOfBoundsException.class, () -> instance.subSequence(-1, 0));
            assertThrowsAny(IndexOutOfBoundsException.class, () -> instance.subSequence(2, 1));
            assertThrowsAny(IndexOutOfBoundsException.class, () -> instance.subSequence(1, 2));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void zeroOut(String type, BytesStore<?, ?> instance) {
        try {
            assertDoesNotThrow(() -> instance.zeroOut(0, 0));
        } finally {
            IOTools.unmonitor(instance);
        }
    }

    private void read(BytesStore<?, ?> instance, final ObjLongConsumer<BytesStore<?, ?>> getter) {
        assumeFalse(instance instanceof NativeBytesStore);
        assertThrowsBufferException(() -> getter.accept(instance, 0));
        assertThrowsAny(IllegalArgumentException.class, () -> getter.accept(instance, -1));
    }

    private void assertThrowsAny(Class<? extends Throwable> tClass, Runnable runnable) {
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
