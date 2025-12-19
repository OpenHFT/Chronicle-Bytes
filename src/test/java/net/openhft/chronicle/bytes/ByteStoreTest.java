/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.core.io.ReferenceOwner.INIT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
public class ByteStoreTest extends BytesTestCommon {

    private static final int SIZE = 128;
    private Bytes<?> bytes;
    private BytesStore<?, ?> bytesStore;

    @AfterEach
    @Override
    public void afterChecks() {
        bytes.releaseLast();
        super.afterChecks();
    }

    @BeforeEach
    public void beforeTest() {
        bytesStore = BytesStore.wrap(ByteBuffer.allocate(SIZE).order(ByteOrder.nativeOrder()));
        bytes = bytesStore.bytesForWrite();
        bytesStore.release(INIT);
        bytes.clear();
    }

    @Test
    public void testReadIncompleteLong() {
        bytes.writeLong(0x0102030405060708L);
        assertEquals(0x0102030405060708L, bytes.readIncompleteLong(0), "readIncompleteLong should read full 8-byte long value from byte store");
        bytes.clear();

        long l = 0;
        for (int i = 1; i <= 8; i++) {
            bytes.writeUnsignedByte(i);
            l |= (long) i << (i * 8 - 8);
            assertEquals(l, bytes.readIncompleteLong(0), "readIncompleteLong should correctly read partial long with " + i + " bytes available");
        }
    }

    @Test
    public void testCAS() {
        assumeFalse(Jvm.isArm(), "TODO FIX");
        final BytesStore<?, ?> bytes = BytesStore.wrap(ByteBuffer.allocate(100));
        bytes.compareAndSwapLong(0, 0L, 1L);
        assertEquals(1L, bytes.readLong(0), "byte store should contain updated value after successful compareAndSwapLong operation");
        bytes.releaseLast();
    }

    @Test
    public void testRead() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte(i, i);
        bytes.writePosition(bytes.capacity());
        for (int i = 0; i < bytes.capacity(); i++)
            assertEquals((byte) i, bytes.readByte(), "sequential byte read should return values in write order");
        for (int i = (int) (bytes.capacity() - 1); i >= 0; i--) {
            assertEquals((byte) i, bytes.readByte(i), "random access byte read should return value written at offset " + i);
        }
    }

    @Test
    public void testReadFully() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte((byte) i);

        @NotNull byte[] bytes = new byte[(int) this.bytes.capacity()];
        this.bytes.read(bytes);
        for (int i = 0; i < this.bytes.capacity(); i++)
            assertEquals((byte) i, bytes[i], "bulk read into byte array should transfer all written bytes in correct order");
    }

    @Test
    public void testCompareAndSetInt() {
        assertTrue(bytes.compareAndSwapInt(0, 0, 1), "compareAndSwapInt should succeed when expected value matches current value at offset 0");
        assertFalse(bytes.compareAndSwapInt(0, 0, 1), "compareAndSwapInt should fail when expected value does not match current value");
        assertTrue(bytes.compareAndSwapInt(8, 0, 1), "compareAndSwapInt should succeed at different offset with matching expected value");
        assertTrue(bytes.compareAndSwapInt(0, 1, 2), "compareAndSwapInt should succeed when updating from 1 to 2 at offset 0");
    }

    @Test
    public void testCompareAndSetLong() {
        assumeFalse(Jvm.isArm(), "TODO FIX");

        assertTrue(bytes.compareAndSwapLong(0L, 0L, 1L), "compareAndSwapLong should succeed when expected value matches current value at offset 0");
        assertFalse(bytes.compareAndSwapLong(0L, 0L, 1L), "compareAndSwapLong should fail when expected value does not match current value");
        assertTrue(bytes.compareAndSwapLong(8L, 0L, 1L), "compareAndSwapLong should succeed at different offset with matching expected value");
        assertTrue(bytes.compareAndSwapLong(0L, 1L, 2L), "compareAndSwapLong should succeed when updating from 1 to 2 at offset 0");
    }

    @Test
    public void testPosition() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte((byte) i);
        for (int i = (int) (bytes.capacity() - 1); i >= 0; i--) {
            bytes.readPosition(i);
            assertEquals((byte) i, bytes.readByte(), "setting read position should allow sequential read from specified offset");
        }
    }

    @Test
    public void testCapacity() {
        assertEquals(SIZE, bytes.capacity(), "byte store wrapped from fixed buffer should report correct capacity");
        final VanillaBytes<Void> bytes = Bytes.allocateDirect(10);
        try {
            assertEquals(10, bytes.capacity(), "directly allocated byte store should have requested capacity");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testRemaining() {
        assertEquals(0, bytes.readRemaining(), "read remaining should be zero when no data has been written");
        assertEquals(SIZE, bytes.writeRemaining(), "write remaining should equal capacity when buffer is empty");
        bytes.writePosition(10);
        assertEquals(10, bytes.readRemaining(), "read remaining should equal write position after writing");
        assertEquals(SIZE - 10, bytes.writeRemaining(), "write remaining should be capacity minus write position");
    }

    @Test
    public void testByteOrder() {
        assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder(), "byte store should use native byte order for multi-byte primitives");
    }

    /*    @Test
        public void testWriteReadBytes() {
            byte[] bytes = "Hello World!".getBytes(ISO_8859_1);
            this.bytes.write(bytes);
            byte[] bytes2 = new byte[bytes.length];
            this.bytes.position(0);
            this.bytes.read(bytes2);
            assertTrue(Arrays.equals(bytes, bytes2));

            this.bytes.write(22, bytes);
            byte[] bytes3 = new byte[bytes.length];
            this.bytes.skipBytes((int) (22 - this.bytes.position()));
            assertEquals(bytes3.length, this.bytes.read(bytes3));
            assertTrue(Arrays.equals(bytes, bytes3));
            this.bytes.position(this.bytes.capacity());
            assertEquals(-1, this.bytes.read(bytes3));
        }*/
    @Test
    public void testWriteReadUtf8()
            throws IORuntimeException {
        bytes.writeUtf8(null);
        final String[] words = {"Hello", "World!", "Bye£€!", ""};
        for (String word : words) {
            bytes.writeUtf8(word);
        }

        assertNull(bytes.readUtf8(), "readUtf8 should return null when null was written");
        for (String word : words) {
            assertEquals(word, bytes.readUtf8(), "readUtf8 should return exact string including UTF-8 multi-byte characters");
        }
        assertEquals("", bytes.readUtf8(), "readUtf8 should correctly handle empty string");
        assertEquals(26, bytes.readPosition(), "read position should advance by total UTF-8 encoded bytes"); // check the size

        bytes.readPosition(0);
        final StringBuilder sb = new StringBuilder();
        assertFalse(bytes.readUtf8(sb), "readUtf8 into StringBuilder should return false for null value");
        for (String word : words) {
            assertTrue(bytes.readUtf8(sb), "readUtf8 into StringBuilder should return true for non-null string");
            assertEquals(word, sb.toString(), "StringBuilder should contain decoded UTF-8 string");
        }
        assertTrue(bytes.readUtf8(sb), "readUtf8 into StringBuilder should return true for empty string");
        assertEquals("", sb.toString(), "StringBuilder should be empty after reading empty string");
    }

    @Test
    public void testWriteReadUTF() {
        final String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.writeUtf8(word);
        }
        bytes.writeUtf8("");
        bytes.writeUtf8(null);
        assertEquals(26, bytes.writePosition(), "write position should reflect total bytes written for UTF-8 encoded strings"); // check the size, more bytes for less strings than writeUtf8

        for (String word : words) {
            assertEquals(word, bytes.readUtf8(), "readUtf8 should return each string in write order with correct UTF-8 decoding");
        }
        assertEquals("", bytes.readUtf8(), "readUtf8 should correctly read empty string");
        assertNull(bytes.readUtf8(), "readUtf8 should return null when null was written");
    }

    @Test
    public void testWriteReadByteBuffer() {
        final byte[] bytes = "Hello\nWorld!\r\nBye".getBytes(ISO_8859_1);
        this.bytes.writeSome(ByteBuffer.wrap(bytes));

        final byte[] bytes2 = new byte[bytes.length + 1];
        final ByteBuffer bb2 = ByteBuffer.wrap(bytes2);
        this.bytes.read(bb2);

        assertEquals(bytes.length, bb2.position(), "ByteBuffer position should advance by number of bytes read from byte store");
        final byte[] bytes2b = Arrays.copyOf(bytes2, bytes.length);
        assertArrayEquals(bytes, bytes2b, "bytes read from byte store into ByteBuffer should match original bytes written");
    }

    @Test
    public void testReadWriteBoolean() {
        for (int i = 0; i < 32; i++)
            bytes.writeBoolean(i, (i & 3) == 0);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i++) {
            final boolean flag = (i & 5) == 0;
            bytes.writeBoolean(flag);
        }

        for (int i = 0; i < 32; i++)
            assertEquals((i & 3) == 0, bytes.readBoolean(), "sequential boolean read should return values in write order");
        for (int i = 32; i < 64; i++) {
            final boolean actual = bytes.readBoolean(i);
            final boolean expected = (i & 5) == 0;
            assertEquals(expected, actual, "random access boolean read should match value written at offset");
        }
    }

    @Test
    public void testReadWriteShort() {
        for (int i = 0; i < 32; i += 2)
            bytes.writeShort(i, (short) i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 2)
            bytes.writeShort((short) i);

        for (int i = 0; i < 32; i += 2)
            assertEquals(i, bytes.readShort(), "sequential short read should return 2-byte values in write order");
        for (int i = 32; i < 64; i += 2)
            assertEquals(i, bytes.readShort(i), "random access short read should return value written at offset");
    }

    @Test
    public void testReadWriteStop()
            throws IORuntimeException {
        final long[] longs = {Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (long i : longs) {
            bytes.writeStopBit(i);
//            Jvm.debug().on(getClass(), i + " " + bytes.position());
        }
        assertEquals(30, bytes.writePosition(), "stop-bit encoding should use 30 bytes total for the four long values");

        for (long i : longs)
            assertEquals(i, bytes.readStopBit(), "readStopBit should correctly decode variable-length encoded long values");
    }

    @Test
    public void testReadWriteUnsignedShort() {
        for (int i = 0; i < 32; i += 2)
            bytes.writeUnsignedShort(i, (~i) & 0xFFFF);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 2)
            bytes.writeUnsignedShort(~i & 0xFFFF);

        for (int i = 0; i < 32; i += 2)
            assertEquals(~i & 0xFFFF, bytes.readUnsignedShort(), "sequential unsigned short read should return 16-bit values without sign extension");
        for (int i = 32; i < 64; i += 2)
            assertEquals(~i & 0xFFFF, bytes.readUnsignedShort(i), "random access unsigned short read should return value written at offset");
    }

    @Test
    public void testReadWriteInt() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeInt(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeInt(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readInt(), "sequential int read should return 4-byte values in write order");
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readInt(i), "random access int read should return value written at offset");
    }

    @Test
    public void testReadWriteThreadSafeInt() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeOrderedInt(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeOrderedInt(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readVolatileInt(), "volatile int read should return values written with ordered semantics");
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readVolatileInt(i), "random access volatile int read should return value written at offset");
    }

    @Test
    public void testReadWriteFloat() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeFloat(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeFloat(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readFloat(), 0, "sequential float read should return 4-byte floating point values in write order");
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readFloat(i), 0, "random access float read should return value written at offset");
    }

    @Test
    public void testReadWriteUnsignedInt()
            throws ArithmeticException, BufferOverflowException, BufferUnderflowException, IllegalArgumentException {
        for (int i = 0; i < 32; i += 4)
            bytes.writeUnsignedInt(i, ~i & 0xFFFF);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeUnsignedInt(~i & 0xFFFF);

        for (int i = 0; i < 32; i += 4)
            assertEquals(~i & 0xFFFFL, bytes.readUnsignedInt(), "sequential unsigned int read should return 32-bit values as long without sign extension");
        for (int i = 32; i < 64; i += 4)
            assertEquals(~i & 0xFFFFL, bytes.readUnsignedInt(i), "random access unsigned int read should return value written at offset");
    }

    @Test
    public void testReadWriteLong() {
        for (long i = 0; i < 32; i += 8)
            bytes.writeLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeLong(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readLong(), "sequential long read should return 8-byte values in write order");
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readLong(i), "random access long read should return value written at offset");
    }

    @Test
    public void testReadWriteThreadSafeLong() {
        assumeFalse(Jvm.isArm(), "TODO FIX");
        for (long i = 0; i < 32; i += 8)
            bytes.writeOrderedLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeOrderedLong(i);
//        Jvm.debug().on(getClass(), bytes.bytes().toDebugString());

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readVolatileLong(), "volatile long read should return values written with ordered semantics");
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readVolatileLong(i), "random access volatile long read should return value written at offset");
    }

    @Test
    public void testReadWriteDouble() {
        for (long i = 0; i < 32; i += 8)
            bytes.writeDouble(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeDouble(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readDouble(), 0, "sequential double read should return 8-byte floating point values in write order");
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readDouble(i), 0, "random access double read should return value written at offset");
    }

    @Test
    public void testReadWriteDoubleAndInt() {
        for (long i = 0; i < 48; i += 12)
            bytes.writeDoubleAndInt(i, (int) i);

        for (long i = 0; i < 48; i += 12) {
            assertEquals(i, bytes.readDouble(), 0, "writeDoubleAndInt should store double in first 8 bytes");
            assertEquals(i, bytes.readInt(), "writeDoubleAndInt should store int in following 4 bytes");
        }
    }

    @Test
    public void testReadWriteStopBitDouble() {
        final double[] doubles = {
                -Double.MAX_VALUE, Double.NEGATIVE_INFINITY,
                Byte.MIN_VALUE, Byte.MAX_VALUE,
                Short.MIN_VALUE, Short.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (double i : doubles) {
            bytes.writeStopBit(i);
            //System.out.println(i + " " + bytes.writePosition());
        }

        for (double i : doubles)
            assertEquals(i, bytes.readStopBitDouble(), 0.0, "readStopBitDouble should correctly decode variable-length encoded floating point values");
    }

    @Test
    public void testStream()
            throws IOException {
        final BytesStore<?, ByteBuffer> bytes0 = BytesStore.wrap(ByteBuffer.allocate(1000));
        final Bytes<?> bytes2 = bytes0.bytesForWrite();
        bytes0.release(INIT);
        try {
            final GZIPOutputStream out = new GZIPOutputStream(bytes2.outputStream());
            out.write("Hello world\n".getBytes(ISO_8859_1));
            out.close();

            try (GZIPInputStream in = new GZIPInputStream(bytes2.inputStream())) {
                final byte[] bytes = new byte[12];
                for (int i = 0; i < 12; i++) {
                    bytes[i] = (byte) in.read();
                }
                assertEquals(-1, in.read(), "input stream should return -1 at end of compressed data");
                assertEquals("Hello world\n", new String(bytes, ISO_8859_1), "byte store should preserve data written through GZIP stream wrapper");
            }
        } finally {
            bytes2.releaseLast();
        }
    }

    @Test
    public void testStream2()
            throws IOException {
        try (OutputStream out = bytes.outputStream()) {
            out.write(11);
            out.write(22);
            out.write(33);
            out.write(44);
            out.write(55);

            try (InputStream in = bytes.inputStream()) {
                assertFalse(in.markSupported(), "byte store input stream should not support mark/reset operations");
                assertEquals(11, in.read(), "input stream should read first byte written to byte store");
                assertEquals(1, bytes.readPosition(), "input stream read should advance byte store read position");
                assertEquals(22, in.read(), "input stream should read second byte in sequence");
                assertEquals(2, bytes.readPosition(), "read position should be 2 after reading two bytes");
                assertEquals(33, in.read(), "input stream should read third byte in sequence");

                assertEquals(1, in.skip(1), "skip should advance position by requested number of bytes");
                assertEquals(4, bytes.readPosition(), "read position should be 4 after skipping one byte");
                assertEquals(1, bytes.readRemaining(), "one byte should remain after reading and skipping 4 of 5 bytes");
                assertEquals(55, in.read(), "input stream should read final byte after skip");

                assertEquals(-1, in.read(), "input stream should return -1 when all bytes have been read");
            }
        }
    }

    @Test
    public void testAddAndGet() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {
            for (int i = 0; i < 10; i++)
                bytesStore.addAndGetInt(0L, 10);
            assertEquals(100, bytesStore.readInt(0L), "addAndGetInt should atomically accumulate values at offset 0");
            assertEquals(0, bytesStore.readInt(4L), "unwritten offset should contain zero value");

            for (int i = 0; i < 11; i++)
                bytesStore.addAndGetInt(4L, 11);
            assertEquals(100, bytesStore.readInt(0L), "value at offset 0 should remain unchanged when modifying offset 4");
            assertEquals(11 * 11, bytesStore.readInt(4L), "addAndGetInt should atomically accumulate 11 additions of 11 at offset 4");
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testAddAndGetLongNative() {
        assumeFalse(Jvm.isArm(), "TODO FIX");
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {
            checkAddAndGetLong();
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testAddAndGetLong() {
        assumeFalse(Jvm.isArm(), "TODO FIX");
        final BytesStore<?, ?> bytesStore2 = BytesStore.wrap(new byte[128]);
        try {
            checkAddAndGetLong();
        } finally {
            bytesStore2.releaseLast();
        }
    }

    private void checkAddAndGetLong() {
        for (int i = 0; i < 10; i++)
            assertEquals((i + 1L) * 10L, bytesStore.addAndGetLong(0L, 10), "addAndGetLong should return accumulated value after each addition");
        assertEquals(100, bytesStore.readLong(0L), "addAndGetLong should atomically accumulate 10 additions of 10 at offset 0");
        assertEquals(0, bytesStore.readLong(8L), "unwritten offset should contain zero value");

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetLong(8L, 11);
        assertEquals(100, bytesStore.readLong(0L), "value at offset 0 should remain unchanged when modifying offset 8");
        assertEquals(11 * 11, bytesStore.readLong(8L), "addAndGetLong should atomically accumulate 11 additions of 11 at offset 8");
    }

    @Test
    public void testAddAndGetFloat() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {

            for (int i = 0; i < 10; i++)
                bytesStore2.addAndGetFloat(0L, 10);
            assertEquals(100, bytesStore2.readFloat(0L), 0f, "addAndGetFloat should atomically accumulate floating point values at offset 0");
            assertEquals(0, bytesStore2.readVolatileFloat(4L), 0f, "unwritten offset should contain zero float value");

            for (int i = 0; i < 11; i++)
                bytesStore2.addAndGetFloat(4L, 11);
            assertEquals(100, bytesStore2.readVolatileFloat(0L), 0f, "value at offset 0 should remain unchanged when modifying offset 4");
            assertEquals(11 * 11, bytesStore2.readFloat(4L), 0f, "addAndGetFloat should atomically accumulate 11 additions of 11 at offset 4");
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testAddAndGetDouble() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {

            for (int i = 0; i < 10; i++)
                bytesStore2.addAndGetDouble(0L, 10);
            assertEquals(100, bytesStore2.readDouble(0L), 0.0, "addAndGetDouble should atomically accumulate double precision values at offset 0");
            assertEquals(0, bytesStore2.readVolatileDouble(8L), 0.0, "unwritten offset should contain zero double value");

            for (int i = 0; i < 11; i++)
                bytesStore2.addAndGetDouble(8L, 11);
            assertEquals(100, bytesStore2.readVolatileDouble(0L), 0.0, "value at offset 0 should remain unchanged when modifying offset 8");
            assertEquals(11 * 11, bytesStore2.readDouble(8L), 0.0, "addAndGetDouble should atomically accumulate 11 additions of 11 at offset 8");
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testToString() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        BytesStore<?, Void> bytes0 = BytesStore.nativeStore(32);
        final Bytes<?> bytes = bytes0.bytesForWrite();
        bytes0.release(INIT);
        try {
            assertEquals("[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show empty byte store with position markers");
            bytes.writeUnsignedByte(1);
            System.gc();
            assertEquals(1, bytes.refCount(), "reference count should be 1 after initial allocation and GC");
            assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ǁ⒈‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show written byte with read limit at position 1");
            bytes.writeUnsignedByte(2);
            bytes.readByte();
            assertEquals("[pos: 1, rlim: 2, wlim: 8EiB, cap: 8EiB ] ⒈ǁ⒉‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show read position advanced after reading one byte");
            bytes.writeUnsignedByte(3);
            assertEquals("[pos: 1, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈ǁ⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show three bytes written with read position at 1");
            bytes.writeUnsignedByte(4);
            bytes.readByte();
            assertEquals("[pos: 2, rlim: 4, wlim: 8EiB, cap: 8EiB ] ⒈⒉ǁ⒊⒋‡٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show read position advanced to 2");
            bytes.writeUnsignedByte(5);
            assertEquals("[pos: 2, rlim: 5, wlim: 8EiB, cap: 8EiB ] ⒈⒉ǁ⒊⒋⒌‡٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show five bytes written with read position at 2");
            bytes.writeUnsignedByte(6);
            bytes.readByte();
            System.gc();
            assertEquals(1, bytes.refCount(), "reference count should remain 1 after GC with active reference");
            assertEquals("[pos: 3, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍‡٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show read position advanced to 3");
            bytes.writeUnsignedByte(7);
            assertEquals("[pos: 3, rlim: 7, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍⒎‡٠٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show seven bytes written");
            bytes.writeUnsignedByte(8);
            assertEquals("[pos: 3, rlim: 8, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍⒎⒏‡٠٠٠٠٠٠٠٠", bytes.toDebugString(), "debug string should show eight bytes written with read position at 3");
        } finally {
            bytes.releaseLast();
            assertEquals(0, bytes.refCount(), "reference count should be zero after releasing last reference");
        }
    }

    @Test
    public void testOverflowReadUtf8()
            throws IORuntimeException {
        final BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        BytesInternal.writeStopBit(bs, 10, 30);
        try {
            assertThrows(BufferUnderflowException.class, () -> bs.readUtf8(10, new StringBuilder()));
        } finally {
            bs.releaseLast();
        }
    }

    @Test
    public void testCopyTo() {
        final BytesStore<?, ?> bytesStoreOriginal = BytesStore.wrap(new byte[SIZE]);
        try {
            for (int i = 0; i < SIZE; i++) {
                final byte randomByte = (byte) ThreadLocalRandom.current().nextInt(Byte.MAX_VALUE);
                bytesStoreOriginal.writeByte(i, randomByte);
            }
            final BytesStore<?, ?> bytesStoreCopy = BytesStore.wrap(new byte[SIZE]);
            try {
                bytesStoreOriginal.copyTo(bytesStoreCopy);
                for (int i = 0; i < SIZE; i++)
                    assertEquals(bytesStoreOriginal.readByte(i), bytesStoreCopy.readByte(i), "copyTo should transfer all bytes from source to destination byte store");
            } finally {
                bytesStoreCopy.releaseLast();
            }
        } finally {
            bytesStoreOriginal.releaseLast();
        }
    }

    @Test
    public void testCopyToDestOffset() {
        final BytesStore<?, ?> bytesStoreOriginal = BytesStore.wrap(new byte[SIZE]);
        try {
            for (int i = 0; i < SIZE; i++) {
                final byte randomByte = (byte) i;
                bytesStoreOriginal.writeByte(i, randomByte);
            }
            int destOffset = 2;
            final Bytes<?> bytesStoreCopy = Bytes.wrapForWrite(new byte[SIZE]);
            bytesStoreCopy.writePosition(destOffset);
            try {
                long bytesCopied = bytesStoreOriginal.copyTo(bytesStoreCopy);
                assertEquals(SIZE - destOffset, bytesCopied, "copyTo should copy only bytes that fit in destination from write position");
                for (int i = 0; i < bytesCopied; i++)
                    assertEquals(bytesStoreOriginal.readByte(i), bytesStoreCopy.readByte(i + destOffset), "copied bytes should match source data at destination offset");
            } finally {
                bytesStoreCopy.releaseLast();
            }
        } finally {
            bytesStoreOriginal.releaseLast();
        }
    }

    @Test
    public void testEmpty() {
        assertEquals(0, BytesStore.empty().realCapacity(), "empty byte store singleton should have zero capacity");
    }

    @Test
    public void testClearAndPadTooMuch() {
        assertThrows(DecoratedBufferOverflowException.class, () -> {
            final Bytes<?> b = bytesStore.bytesForWrite();
            try {
                b.clearAndPad(SIZE + 1);
            } finally {
                b.releaseLast();
            }
        });
    }

    @Test
    public void testFollow() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        ByteBuffer direct = ByteBuffer.allocateDirect(128);
        for (int i = 0; i < 128; i++) {
            BytesStore<?, ByteBuffer> store = BytesStore.follow(direct);
            store.write(i, new byte[]{(byte) i});
            store.releaseLast();
        }

        for (int i = 0; i < 128; i++) {
            assertEquals(i, direct.get(i), "BytesStore.follow should create wrapper that writes directly to underlying ByteBuffer");
        }
    }
}
