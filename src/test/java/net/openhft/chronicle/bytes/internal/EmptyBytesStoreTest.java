/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.RandomDataOutput;
import net.openhft.chronicle.core.io.IOTools;
import org.opentest4j.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.ObjLongConsumer;
import java.util.stream.Stream;

import static net.openhft.chronicle.bytes.Bytes.elasticHeapByteBuffer;
import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for empty BytesStore instances because zero-capacity stores must
 * reject reads and writes to avoid undefined behaviour in native memory access.
 * These tests validate boundary conditions, sentinel values, and exception handling
 * in order to ensure robustness of the empty store implementations.
 * Required by the API contract to verify that empty stores do not permit operations.
 */
@SuppressWarnings({"checkstyle:MMLacksPurpose", "deprecation", "checkstyle:MMOverusedWord", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("Empty BytesStore boundary and sentinel validation")
public class EmptyBytesStoreTest extends BytesTestCommon {

    private BytesStore<?, ?> instance;

    static Stream<BytesStore<?, ?>> data() {
        return Stream.of(
                Bytes.empty(),
                BytesStore.empty(),
                NativeBytesStore.nativeStoreWithFixedCapacity(0),
                NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForRead(),
                NativeBytesStore.nativeStoreWithFixedCapacity(0).bytesForWrite()
        );
    }

    @AfterEach
    public void teardown() {
        if (instance != null) {
            IOTools.unmonitor(instance);
            instance = null;
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store differs from wrapped empty byte array")
    @MethodSource("data")
    public void notSameAsEmpty(BytesStore<?, ?> instance) {
        this.instance = instance;
        // a case which should produce a different instance. Wire depends on this
        assertNotSame(BytesStore.wrap(new byte[0]), instance,
                "Empty store should not match a wrapped byte array instance");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store retains a non-zero reference count")
    @MethodSource("data")
    public void refCount(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertNotEquals(0, instance.refCount(),
                "Empty store retains a reference count");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeByte(int) on zero capacity")
    @MethodSource("data")
    public void writeByteInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeByte(int) is verified on non-native empty stores");
        expectBufferException("Empty store writeByte(int) should throw buffer exception",
                () -> instance.writeByte(0, 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeByte(byte) on zero capacity")
    @MethodSource("data")
    public void writeByte(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeByte(byte) is verified on non-native empty stores");
        expectBufferException("Empty store writeByte(byte) should throw buffer exception",
                () -> instance.writeByte(0, (byte) 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeShort on zero capacity")
    @MethodSource("data")
    public void writeShort(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeShort is verified on non-native empty stores");
        expectBufferException("Empty store writeShort should throw buffer exception",
                () -> instance.writeShort(0, (short) 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeInt on zero capacity")
    @MethodSource("data")
    public void writeInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeInt is verified on non-native empty stores");
        expectBufferException("Empty store writeInt should throw buffer exception",
                () -> instance.writeInt(0, 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeOrderedInt on zero capacity")
    @MethodSource("data")
    public void writeOrderedInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeOrderedInt is verified on non-native empty stores");
        expectBufferException("Empty store writeOrderedInt should throw buffer exception",
                () -> instance.writeOrderedInt(0, 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeLong on zero capacity")
    @MethodSource("data")
    public void writeLong(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeLong is verified on non-native empty stores");
        expectBufferException("Empty store writeLong should throw buffer exception",
                () -> instance.writeLong(0, 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeOrderedLong on zero capacity")
    @MethodSource("data")
    public void writeOrderedLong(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeOrderedLong is verified on non-native empty stores");
        expectBufferException("Empty store writeOrderedLong should throw buffer exception",
                () -> instance.writeOrderedLong(0, 0L));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeFloat on zero capacity")
    @MethodSource("data")
    public void writeFloat(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeFloat is verified on non-native empty stores");
        expectBufferException("Empty store writeFloat should throw buffer exception",
                () -> instance.writeFloat(0, 0.0f));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeDouble on zero capacity")
    @MethodSource("data")
    public void writeDouble(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeDouble is verified on non-native empty stores");
        expectBufferException("Empty store writeDouble should throw buffer exception",
                () -> instance.writeDouble(0, 0.0d));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeVolatileByte on zero capacity")
    @MethodSource("data")
    public void writeVolatileByte(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeVolatileByte is verified on non-native empty stores");
        expectBufferException("Empty store writeVolatileByte should throw buffer exception",
                () -> instance.writeVolatileByte(0, (byte) 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeVolatileShort on zero capacity")
    @MethodSource("data")
    public void writeVolatileShort(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeVolatileShort is verified on non-native empty stores");
        expectBufferException("Empty store writeVolatileShort should throw buffer exception",
                () -> instance.writeVolatileShort(0, (short) 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeVolatileInt on zero capacity")
    @MethodSource("data")
    public void writeVolatileInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeVolatileInt is verified on non-native empty stores");
        expectBufferException("Empty store writeVolatileInt should throw buffer exception",
                () -> instance.writeVolatileInt(0, 0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store rejects writeVolatileLong on zero capacity")
    @MethodSource("data")
    public void writeVolatileLong(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "writeVolatileLong is verified on non-native empty stores");
        expectBufferException("Empty store writeVolatileLong should throw buffer exception",
                () -> instance.writeVolatileLong(0, 0L));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store write(byte[], offset, length) honours zero length")
    @MethodSource("data")
    public void write(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertDoesNotThrow(() -> instance.write(0, new byte[1], 0, 0),
                "Empty store ignores zero length byte array write");
        assumeFalse(instance instanceof NativeBytesStore,
                "write(byte[], offset, length) is verified on non-native empty stores");
        expectBufferException("Empty store write with length should throw buffer exception",
                () -> instance.write(0, new byte[1], 0, 1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store write(Bytes, offset, length) honours zero length")
    @MethodSource("data")
    public void write2(BytesStore<?, ?> instance) {
        this.instance = instance;
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
        bytes.append("Hello");
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes, 0, 0),
                    "Empty store ignores zero length bytes write");
            assumeFalse(instance instanceof NativeBytesStore,
                    "write(Bytes, offset, length) is verified on non-native empty stores");
            expectBufferException("Empty store write bytes with length should throw buffer exception",
                    () -> instance.write(0, bytes, 0, 1));
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store write(byte[]) rejects writes on zero capacity")
    @MethodSource("data")
    public void write3(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "write(byte[]) is verified on non-native empty stores");
        expectBufferException("Empty store write byte array should throw buffer exception",
                () -> instance.write(0, new byte[1]));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store write(Bytes) rejects writes on zero capacity")
    @MethodSource("data")
    public void write4(BytesStore<?, ?> instance) {
        this.instance = instance;
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
        try {
            assertDoesNotThrow(() -> instance.write(0, bytes),
                    "Empty store ignores bytes write with zero length");
            bytes.append("Hello");
            assumeFalse(instance instanceof NativeBytesStore,
                    "write(Bytes) is verified on non-native empty stores");
            expectBufferException("Empty store write bytes at zero should throw buffer exception",
                    () -> instance.write(0, bytes));
            expectBufferException("Empty store write bytes at offset should throw buffer exception",
                    () -> instance.write(1, bytes));
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readByte rejects reads at zero capacity")
    @MethodSource("data")
    public void readByte(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readByte);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store peekUnsignedByte returns negative sentinel value")
    @MethodSource("data")
    public void peekUnsignedByte(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertEquals(-1, instance.peekUnsignedByte(0),
                "Empty store returns negative for peek unsigned byte");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readShort rejects reads at zero capacity")
    @MethodSource("data")
    public void readShort(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readShort);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readInt rejects reads at zero capacity")
    @MethodSource("data")
    public void readInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readLong);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readLong rejects reads at zero capacity")
    @MethodSource("data")
    public void readLong(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readLong);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readFloat rejects reads at zero capacity")
    @MethodSource("data")
    public void readFloat(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readFloat);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readDouble rejects reads at zero capacity")
    @MethodSource("data")
    public void readDouble(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readDouble);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readVolatileByte rejects reads at zero capacity")
    @MethodSource("data")
    public void readVolatileByte(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readVolatileByte);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readVolatileShort rejects reads at zero capacity")
    @MethodSource("data")
    public void readVolatileShort(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readVolatileShort);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readVolatileInt rejects reads at zero capacity")
    @MethodSource("data")
    public void readVolatileInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readVolatileInt);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store readVolatileLong rejects reads at zero capacity")
    @MethodSource("data")
    public void readVolatileLong(BytesStore<?, ?> instance) {
        this.instance = instance;
        read(BytesStore::readVolatileLong);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store hashCode matches native empty store")
    @MethodSource("data")
    public void hashCodeTest(BytesStore<?, ?> instance) {
        this.instance = instance;
        int actual = instance.hashCode();
        int expected = NativeBytesStore.from("").hashCode();
        assertEquals(expected, actual,
                "Empty store returns expected substring");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store equality matches native empty store")
    @MethodSource("data")
    public void equalsTest(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertNotEquals(null, instance,
                "Empty store should not equal null");
        assertNotEquals(instance, null,
                "Null should not equal empty store");
        assertEquals(NativeBytesStore.from(""), instance,
                "Empty store equals native store from empty string");
        assertEquals(instance, NativeBytesStore.from(""),
                "Native store from empty string equals empty store");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store copy returns the same singleton instance")
    @MethodSource("data")
    public void copy(BytesStore<?, ?> instance) {
        this.instance = instance;
        final BytesStore<?, Void> copy = uncheckedCast(instance.copy());
        assertEquals(instance, copy,
                "Copy returns same empty instance");
        copy.releaseLast();
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store bytesForRead has zero capacity")
    @MethodSource("data")
    public void bytesForRead(BytesStore<?, ?> instance) {
        this.instance = instance;
        final Bytes<Void> bytes = uncheckedCast(instance.bytesForRead());
        try {
            assertEquals(0, bytes.capacity(),
                    "Empty bytes capacity stays zero");
            assertEquals(0, bytes.readPosition(),
                    "Empty bytes read position stays zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store capacity remains at zero bytes")
    @MethodSource("data")
    public void capacity(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertEquals(0, instance.capacity(),
                "Empty store capacity stays at zero bytes");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store reports no backing object reference")
    @MethodSource("data")
    public void underlyingObject(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertNull(instance.underlyingObject(),
                "Empty store exposes no underlying object reference");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store inside checks handle bounds")
    @MethodSource("data")
    public void inside(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertTrue(instance.inside(0, 0),
                "Empty store inside check is true for zero length");
        assertFalse(instance.inside(0, 1),
                "Empty store inside check fails for length one");
        assertFalse(instance.inside(1, 0),
                "Empty store inside check fails for non zero offset");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store inside(length) fails for zero capacity")
    @MethodSource("data")
    public void testInside(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertFalse(instance.inside(0),
                "Empty store inside check fails for default length");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store copyTo leaves targets unchanged")
    @MethodSource("data")
    public void copyTo(BytesStore<?, ?> instance) {
        this.instance = instance;
        final Bytes<ByteBuffer> bytes = elasticHeapByteBuffer();
        try {
            assertDoesNotThrow(() -> instance.copyTo(bytes),
                    "Empty store copy to bytes does not throw");
        } finally {
            bytes.releaseLast();
        }

        final byte[] arr = new byte[1];
        arr[0] = 13;
        assertDoesNotThrow(() -> instance.copyTo(arr),
                "Empty store copy to array does not throw");
        assertEquals(13, arr[0],
                "Copy to array does not change sentinel value");

        final ByteBuffer bb = ByteBuffer.allocate(1);
        assertDoesNotThrow(() -> instance.copyTo(bb),
                "Empty store copy to byte buffer does not throw");
        assertEquals(0, bb.position(),
                "Byte buffer position remains unchanged after copy");

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> instance.copyTo(os),
                "Empty store copy to output stream does not throw");
        final byte[] toByteArray = os.toByteArray();
        assertEquals(0, toByteArray.length,
                "Output stream remains empty after copy");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store nativeWrite validates offsets and lengths")
    @MethodSource("data")
    public void nativeWrite(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertThrows("nativeWrite rejects negative offset",
                IllegalArgumentException.class, () -> instance.nativeWrite(34, -1, 0));
        assertThrows("nativeWrite rejects negative length",
                IllegalArgumentException.class, () -> instance.nativeWrite(34, 0, -1));
        assertDoesNotThrow(() -> instance.nativeWrite(34, 0, 0),
                "Empty store native write ignores zero length");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store write8bit rejects writes on zero capacity")
    @MethodSource("data")
    public void write8bit(BytesStore<?, ?> instance) {
        this.instance = instance;
        final BytesStore<?, ?> bs = BytesStore.from("A");
        assertThrows("write8bit rejects negative offset",
                IllegalArgumentException.class, () -> instance.write8bit(-1, bs));
        assumeFalse(instance instanceof NativeBytesStore,
                "write8bit is verified on non-native empty stores");
        assertThrows("write8bit overflows empty store",
                BufferOverflowException.class, () -> instance.write8bit(0, bs));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store write8bit(CharSequence) rejects writes")
    @MethodSource("data")
    public void testWrite8bit(BytesStore<?, ?> instance) {
        this.instance = instance;
        expectBufferException("Empty store write8bit should throw buffer exception",
                () -> instance.write8bit(0, "A", 0, 1));
        assertThrows("write8bit rejects negative offset and length",
                IllegalArgumentException.class, () -> instance.write8bit(-1, "A", -1, 0));
        assertThrows("write8bit rejects negative length",
                IllegalArgumentException.class, () -> instance.write8bit(0, "A", 0, -1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store nativeRead validates offsets and lengths")
    @MethodSource("data")
    public void nativeRead(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "nativeRead is verified on non-native empty stores");
        expectBufferException("Empty store nativeRead should throw buffer exception",
                () -> instance.nativeRead(0, 1, 1));
        assertThrows("nativeRead rejects negative offset",
                IllegalArgumentException.class, () -> instance.nativeRead(-1, 1, 0));
        assertThrows("nativeRead rejects negative length",
                IllegalArgumentException.class, () -> instance.nativeRead(0, 1, -1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store compareAndSwapInt rejects writes on zero capacity")
    @MethodSource("data")
    public void compareAndSwapInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "compareAndSwapInt is verified on non-native empty stores");
        expectBufferException("Empty store compareAndSwapInt should throw buffer exception",
                () -> instance.compareAndSwapInt(0, 1, 1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store compareAndSwapLong rejects writes on zero capacity")
    @MethodSource("data")
    public void compareAndSwapLong(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "compareAndSwapLong is verified on non-native empty stores");
        expectBufferException("Empty store compareAndSwapLong should throw buffer exception",
                () -> instance.compareAndSwapLong(0, 1L, 1L));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store compareAndSwapDouble rejects writes on zero capacity")
    @MethodSource("data")
    public void compareAndSwapDouble(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "compareAndSwapDouble is verified on non-native empty stores");
        expectBufferException("Empty store compareAndSwapDouble should throw",
                () -> instance.compareAndSwapDouble(0, 1d, 1d));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store compareAndSwapFloat rejects writes on zero capacity")
    @MethodSource("data")
    public void compareAndSwapFloat(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "compareAndSwapFloat is verified on non-native empty stores");
        expectBufferException("Empty store compareAndSwapFloat should throw",
                () -> instance.compareAndSwapFloat(0, 1f, 1f));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store testAndSetInt rejects writes on zero capacity")
    @MethodSource("data")
    public void testAndSetInt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "testAndSetInt is verified on non-native empty stores");
        expectBufferException("Empty store testAndSetInt should throw",
                () -> instance.testAndSetInt(0, 1, 1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("zero-capacity store equalBytes validates offset and content matching")
    @MethodSource("data")
    public void equalBytes(BytesStore<?, ?> instance) {
        this.instance = instance;
        final BytesStore<?, ?> bs = BytesStore.from("A");
        final BytesStore<?, ?> emptyBs = BytesStore.from("");
        try {
            assertTrue(instance.equalBytes(bs, 0),
                    "Empty store equals bytes store at offset zero");
            assertFalse(instance.equalBytes(emptyBs, 1),
                    "Empty store unequal when offset is beyond length");
            assertTrue(instance.equalBytes(emptyBs, 0),
                    "Empty store equals empty bytes store at offset zero");
            assumeFalse(instance instanceof NativeBytesStore,
                    "equalBytes negative offset is verified on non-native empty stores");
            assertThrows("equalBytes rejects negative offset",
                    IllegalArgumentException.class, () -> instance.equalBytes(bs, -1));
        } finally {
            bs.releaseLast();
            emptyBs.releaseLast();
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store move rejects out-of-bounds operations")
    @MethodSource("data")
    public void move(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "move is verified on non-native empty stores");
        expectBufferException("Empty store move with length throws buffer exception",
                () -> instance.move(0, 0, 1));
        assertThrows("Move rejects negative from offset",
                IllegalArgumentException.class, () -> instance.move(-1, 0, 0));
        assertThrows("Move rejects negative to offset",
                IllegalArgumentException.class, () -> instance.move(0, -1, 0));
        assertThrows("Move rejects negative length",
                IllegalArgumentException.class, () -> instance.move(0, 0, -1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store addressForRead fails for out of bounds access")
    @MethodSource("data")
    public void addressForRead(BytesStore<?, ?> instance) {
        this.instance = instance;
        expectBufferException("Empty store addressForRead out of bounds throws",
                () -> instance.addressForRead(1));
        assertThrows("addressForRead rejects negative index",
                IllegalArgumentException.class, () -> instance.addressForRead(-1));
        assumeFalse(instance.isDirectMemory(),
                "addressForRead requires heap-backed empty store for buffer exception");
        expectBufferException("addressForRead requires direct memory backing",
                () -> instance.addressForRead(0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store addressForWrite fails for out of bounds access")
    @MethodSource("data")
    public void addressForWrite(BytesStore<?, ?> instance) {
        this.instance = instance;
        expectBufferException("Empty store addressForWrite out of bounds throws",
                () -> instance.addressForWrite(1));
        assertThrows("addressForWrite rejects negative index",
                IllegalArgumentException.class, () -> instance.addressForWrite(-1));
        assumeFalse(instance.isDirectMemory(),
                "addressForWrite requires heap-backed empty store for buffer exception");
        expectBufferException("addressForWrite requires direct memory backing",
                () -> instance.addressForWrite(0));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store addressForWritePosition rejects zero capacity")
    @MethodSource("data")
    public void addressForWritePosition(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "addressForWritePosition is verified on non-native empty stores");
        assumeFalse(instance.bytesStore() instanceof NativeBytesStore,
                "addressForWritePosition skips native BytesStore wrapper");
        expectBufferException("Empty store addressForWritePosition throws",
                instance::addressForWritePosition);
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store bytesForWrite rejects writeSkip on zero capacity")
    @MethodSource("data")
    public void bytesForWrite(BytesStore<?, ?> instance) {
        this.instance = instance;
        try {
            final Bytes<?> bytes = instance.bytesForWrite();
            IOTools.unmonitor(bytes);
            expectBufferException("Empty bytes for write reject skip",
                    () -> bytes.writeSkip(1));
        } catch (UnsupportedOperationException ignored) {
            assertNotNull(ignored,
                    "Unsupported operation is expected for bytesForWrite");
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store reports no shared memory backing")
    @MethodSource("data")
    public void sharedMemory(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertFalse(instance.sharedMemory(),
                "Empty store has no shared memory backing");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store immutable capacity remains zero")
    @MethodSource("data")
    public void isImmutableBytesStore(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertEquals(0, instance.capacity(),
                "Empty store capacity remains zero");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store string forms match empty bytes store")
    @MethodSource("data")
    public void testToString(BytesStore<?, ?> instance) {
        this.instance = instance;
        final BytesStore<?, ?> bytes = Bytes.from("");
        final BytesStore<?, ?> bs = bytes.bytesStore();
        assertNotNull(bs,
                "Bytes store should be available for empty bytes");
        try {
            assertEquals(bs.toString(), instance.toString(),
                    "Empty store toString matches empty bytes store");
            assertEquals(bs.toDebugString(), instance.toDebugString(),
                    "Empty store debug string matches empty bytes store");
            assertEquals(bs.toDebugString(2), instance.toDebugString(2),
                    "Empty store debug string depth matches empty bytes store");
            assertEquals(bs.to8bitString(), instance.to8bitString(),
                    "Empty store 8bit string matches empty bytes store");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store chars stream yields no elements")
    @MethodSource("data")
    public void chars(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertEquals(0, instance.chars().count(),
                "Empty store has zero chars");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store codePoints stream yields no elements")
    @MethodSource("data")
    public void codePoints(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertEquals(0, instance.codePoints().count(),
                "Empty store has zero code points");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store length remains at zero characters")
    @MethodSource("data")
    public void length(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertEquals(0, instance.length(),
                "Empty store length stays at zero characters");
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store charAt rejects negative index")
    @MethodSource("data")
    public void charAt(BytesStore<?, ?> instance) {
        this.instance = instance;
        assumeFalse(instance instanceof NativeBytesStore,
                "charAt negative index is verified on non-native empty stores");
        assertThrows("charAt rejects negative index",
                IllegalArgumentException.class, () -> instance.charAt(-1));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store subSequence rejects invalid ranges")
    @MethodSource("data")
    public void subSequence(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertThrows("subSequence rejects negative start index",
                IndexOutOfBoundsException.class, () -> instance.subSequence(-1, 0));
        assertThrows("subSequence rejects start greater than end",
                IndexOutOfBoundsException.class, () -> instance.subSequence(2, 1));
        assertThrows("subSequence rejects end beyond length",
                IndexOutOfBoundsException.class, () -> instance.subSequence(1, 2));
    }

    @ParameterizedTest(name = "{index} {0}")
    @DisplayName("empty store zeroOut ignores zero length ranges")
    @MethodSource("data")
    public void zeroOut(BytesStore<?, ?> instance) {
        this.instance = instance;
        assertDoesNotThrow(() -> instance.zeroOut(0, 0),
                "Empty store zeroOut ignores zero-length range");
        // outside bounds are ignored
    }

    private void read(final ObjLongConsumer<BytesStore<?, ?>> getter) {
        assumeFalse(instance instanceof NativeBytesStore,
                "read helpers verify empty store behaviour for non-native stores");
        expectBufferException("Empty store read with index zero throws buffer exception",
                () -> getter.accept(instance, 0));
        assertThrows("Empty store read rejects negative index",
                IllegalArgumentException.class, () -> getter.accept(instance, -1));
    }

    private void assertThrows(String message, Class<? extends Throwable> tClass, Runnable runnable) {
        try {
            runnable.run();

        } catch (UnsupportedOperationException ignored) {
            return;
        } catch (Throwable t) {
            if (tClass.isInstance(t))
                return;
            throw new AssertionFailedError("Unexpected exception type thrown: " + message,
                    tClass, t.getClass(), t);
        }
        throw new AssertionFailedError("Expected exception " + tClass + " for " + message);
    }

    private void expectBufferException(String message, final Runnable consumer) {
        try {
            consumer.run();

        } catch (BufferOverflowException | BufferUnderflowException | UnsupportedOperationException e) {
            return;
        } catch (Throwable t) {
            throw new AssertionFailedError("Unexpected exception for empty store: " + message, t);
        }
        throw new AssertionFailedError("Missing buffer exception: " + message);
    }
}
