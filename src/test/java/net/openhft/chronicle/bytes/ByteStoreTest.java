/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("ByteStore read and write behaviour coverage tests")
public class ByteStoreTest extends BytesTestCommon {

    private static final int SIZE = 128;
    private Bytes<?> bytes;
    private BytesStore<?, ?> bytesStore;

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
    @DisplayName("Read incomplete long values from written bytes")
    public void testReadIncompleteLong() {
        bytes.writeLong(0x0102030405060708L);
        assertEquals(0x0102030405060708L, bytes.readIncompleteLong(0),
                "readIncompleteLong should return the full long when all bytes are present");
        bytes.clear();

        long l = 0;
        for (int i = 1; i <= 8; i++) {
            bytes.writeUnsignedByte(i);
            l |= (long) i << (i * 8 - 8);
            assertEquals(l, bytes.readIncompleteLong(0),
                    "readIncompleteLong should match partial bytes at length " + i);
        }
    }

    @Test
    @DisplayName("Compare-and-swap updates long values atomically")
    public void testCAS() {
        assumeFalse(Jvm.isArm(), "ARM does not support ByteStore compare-and-swap long test");
        final BytesStore<?, ?> bytes = BytesStore.wrap(ByteBuffer.allocate(100));
        bytes.compareAndSwapLong(0, 0L, 1L);
        assertEquals(1L, bytes.readLong(0), "CAS should update the long at offset 0");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Read sequential and indexed bytes after writes")
    public void testRead() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte(i, i);
        bytes.writePosition(bytes.capacity());
        for (int i = 0; i < bytes.capacity(); i++)
            assertEquals((byte) i, bytes.readByte(), "Sequential read should match write order at index " + i);
        for (int i = (int) (bytes.capacity() - 1); i >= 0; i--) {
            assertEquals((byte) i, bytes.readByte(i), "Indexed read should match stored value at index " + i);
        }
    }

    @Test
    @DisplayName("Read into byte array retains written values")
    public void testReadFully() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte((byte) i);

        @NotNull byte[] bytes = new byte[(int) this.bytes.capacity()];
        this.bytes.read(bytes);
        for (int i = 0; i < this.bytes.capacity(); i++)
            assertEquals((byte) i, bytes[i], "Array read should match byte written at index " + i);
    }

    @Test
    @DisplayName("Compare-and-swap on int values succeeds and fails correctly")
    public void testCompareAndSetInt() {
        assertTrue(bytes.compareAndSwapInt(0, 0, 1), "CAS should update zero to one");
        assertFalse(bytes.compareAndSwapInt(0, 0, 1), "CAS should fail when expected value is stale");
        assertTrue(bytes.compareAndSwapInt(8, 0, 1), "CAS should update at a different offset");
        assertTrue(bytes.compareAndSwapInt(0, 1, 2), "Int CAS should update when expected value matches");
    }

    @Test
    @DisplayName("Compare-and-swap on long values succeeds and fails correctly")
    public void testCompareAndSetLong() {
        assumeFalse(Jvm.isArm(), "ARM does not support compare-and-swap long test");

        assertTrue(bytes.compareAndSwapLong(0L, 0L, 1L), "CAS should update zero to one at offset 0");
        assertFalse(bytes.compareAndSwapLong(0L, 0L, 1L), "CAS should fail after the value is updated");
        assertTrue(bytes.compareAndSwapLong(8L, 0L, 1L), "CAS should update at offset 8");
        assertTrue(bytes.compareAndSwapLong(0L, 1L, 2L), "Long CAS should update when expected value matches");
    }

    @Test
    @DisplayName("Read position updates control sequential reads")
    public void testPosition() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte((byte) i);
        for (int i = (int) (bytes.capacity() - 1); i >= 0; i--) {
            bytes.readPosition(i);
            assertEquals((byte) i, bytes.readByte(), "readPosition should select byte at index " + i);
        }
    }

    @Test
    @DisplayName("Capacity matches allocation size for wrapped stores")
    public void testCapacity() {
        assertEquals(SIZE, bytes.capacity(), "Wrapped ByteBuffer capacity should match test size");
        final VanillaBytes<Void> bytes = Bytes.allocateDirect(10);
        try {
            assertEquals(10, bytes.capacity(), "Allocated direct Bytes should report the requested capacity");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("Read/write remaining values track positions")
    public void testRemaining() {
        assertEquals(0, bytes.readRemaining(), "New bytes should have no readable data");
        assertEquals(SIZE, bytes.writeRemaining(), "Writable space should match capacity at start");
        bytes.writePosition(10);
        assertEquals(10, bytes.readRemaining(), "Readable bytes should match written length");
        assertEquals(SIZE - 10, bytes.writeRemaining(), "Writable space should shrink with writes");
    }

    @Test
    @DisplayName("Byte order matches native order for wrapped bytes")
    public void testByteOrder() {
        assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder(), "Byte order should default to native order");
    }

    @Test
    @DisplayName("Write and read UTF-8 strings with nullable values")
    public void testWriteReadUtf8()
            throws IORuntimeException {
        bytes.writeUtf8(null);
        final String[] words = new String[]{"Hello", "World!", "Bye£€!", ""};
        for (String word : words) {
            bytes.writeUtf8(word);
        }

        assertNull(bytes.readUtf8(), "First UTF-8 read should return null marker");
        for (String word : words) {
            assertEquals(word, bytes.readUtf8(), "UTF-8 read should match written value for word " + word);
        }
        assertEquals("", bytes.readUtf8(), "Trailing empty string should be preserved");
        assertEquals(26, bytes.readPosition(), "Write position should reflect encoded UTF-8 size");

        bytes.readPosition(0);
        final StringBuilder sb = new StringBuilder();
        assertFalse(bytes.readUtf8(sb), "StringBuilder read should return false for null entry");
        for (String word : words) {
            assertTrue(bytes.readUtf8(sb), "StringBuilder read should return true for word " + word);
            assertEquals(word, sb.toString(), "StringBuilder content should match UTF-8 value for word " + word);
        }
        assertTrue(bytes.readUtf8(sb), "StringBuilder read should succeed for empty value");
        assertEquals("", sb.toString(), "StringBuilder should contain empty string");
    }

    @Test
    @DisplayName("Write and read UTF-8 strings with explicit null terminator")
    public void testWriteReadUTF() {
        final String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.writeUtf8(word);
        }
        bytes.writeUtf8("");
        bytes.writeUtf8(null);
        assertEquals(26, bytes.writePosition(),
                "Write position should reflect UTF-8 size including terminators");

        for (String word : words) {
            assertEquals(word, bytes.readUtf8(),
                    "UTF-8 read should match written value for word " + word + " in null-terminated sequence");
        }
        assertEquals("", bytes.readUtf8(), "Empty UTF-8 string should round-trip");
        assertNull(bytes.readUtf8(), "Final UTF-8 entry should be null");
    }

    @Test
    @DisplayName("Write and read ByteBuffer contents through Bytes")
    public void testWriteReadByteBuffer() {
        final byte[] bytes = "Hello\nWorld!\r\nBye".getBytes(ISO_8859_1);
        this.bytes.writeSome(ByteBuffer.wrap(bytes));

        final byte[] bytes2 = new byte[bytes.length + 1];
        final ByteBuffer bb2 = ByteBuffer.wrap(bytes2);
        this.bytes.read(bb2);

        assertEquals(bytes.length, bb2.position(), "ByteBuffer position should advance by written length");
        final byte[] bytes2b = Arrays.copyOf(bytes2, bytes.length);
        assertArrayEquals(bytes, bytes2b, "ByteBuffer content should match written bytes");
    }

    @Test
    @DisplayName("Read and write boolean values by position and cursor")
    public void testReadWriteBoolean() {
        for (int i = 0; i < 32; i++)
            bytes.writeBoolean(i, (i & 3) == 0);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i++) {
            final boolean flag = (i & 5) == 0;
            bytes.writeBoolean(flag);
        }

        for (int i = 0; i < 32; i++)
            assertEquals((i & 3) == 0, bytes.readBoolean(),
                    "Sequential boolean read should match value at index " + i);
        for (int i = 32; i < 64; i++) {
            final boolean actual = bytes.readBoolean(i);
            final boolean expected = (i & 5) == 0;
            assertEquals(expected, actual, "Indexed boolean read should match value at index " + i);
        }
    }

    @Test
    @DisplayName("Read and write short values by position and cursor")
    public void testReadWriteShort() {
        for (int i = 0; i < 32; i += 2)
            bytes.writeShort(i, (short) i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 2)
            bytes.writeShort((short) i);

        for (int i = 0; i < 32; i += 2)
            assertEquals(i, bytes.readShort(), "Sequential short read should match value at index " + i);
        for (int i = 32; i < 64; i += 2)
            assertEquals(i, bytes.readShort(i), "Indexed short read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write stop-bit encoded long values")
    public void testReadWriteStop()
            throws IORuntimeException {
        final long[] longs = {Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (long i : longs) {
            bytes.writeStopBit(i);
        }
        assertEquals(9 + 10 + 5 + 6, bytes.writePosition(),
                "Stop-bit encoding should use expected byte lengths");

        for (long i : longs)
            assertEquals(i, bytes.readStopBit(), "Stop-bit decoded value should match original for value " + i);
    }

    @Test
    @DisplayName("Read and write unsigned short values")
    public void testReadWriteUnsignedShort() {
        for (int i = 0; i < 32; i += 2)
            bytes.writeUnsignedShort(i, (~i) & 0xFFFF);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 2)
            bytes.writeUnsignedShort(~i & 0xFFFF);

        for (int i = 0; i < 32; i += 2)
            assertEquals(~i & 0xFFFF, bytes.readUnsignedShort(),
                    "Sequential unsigned short read should match value at index " + i);
        for (int i = 32; i < 64; i += 2)
            assertEquals(~i & 0xFFFF, bytes.readUnsignedShort(i),
                    "Indexed unsigned short read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write int values by position and cursor")
    public void testReadWriteInt() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeInt(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeInt(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readInt(), "Sequential int read should match value at index " + i);
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readInt(i), "Indexed int read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write ordered int values with volatile reads")
    public void testReadWriteThreadSafeInt() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeOrderedInt(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeOrderedInt(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readVolatileInt(), "Volatile read should match value at index " + i);
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readVolatileInt(i), "Indexed volatile read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write float values by position and cursor")
    public void testReadWriteFloat() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeFloat(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeFloat(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readFloat(), 0, "Sequential float read should match value at index " + i);
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readFloat(i), 0, "Indexed float read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write unsigned int values")
    public void testReadWriteUnsignedInt()
            throws ArithmeticException, BufferOverflowException, BufferUnderflowException, IllegalArgumentException {
        for (int i = 0; i < 32; i += 4)
            bytes.writeUnsignedInt(i, ~i & 0xFFFF);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeUnsignedInt(~i & 0xFFFF);

        for (int i = 0; i < 32; i += 4)
            assertEquals(~i & 0xFFFFL, bytes.readUnsignedInt(),
                    "Sequential unsigned int read should match value at index " + i);
        for (int i = 32; i < 64; i += 4)
            assertEquals(~i & 0xFFFFL, bytes.readUnsignedInt(i),
                    "Indexed unsigned int read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write long values by position and cursor")
    public void testReadWriteLong() {
        for (long i = 0; i < 32; i += 8)
            bytes.writeLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeLong(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readLong(), "Sequential long read should match value at index " + i);
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readLong(i), "Indexed long read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write ordered long values with volatile reads")
    public void testReadWriteThreadSafeLong() {
        assumeFalse(Jvm.isArm(), "ARM does not support ordered long reads in this test");
        for (long i = 0; i < 32; i += 8)
            bytes.writeOrderedLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeOrderedLong(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readVolatileLong(), "Volatile long read should match value at index " + i);
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readVolatileLong(i), "Indexed volatile long read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write double values by position and cursor")
    public void testReadWriteDouble() {
        for (long i = 0; i < 32; i += 8)
            bytes.writeDouble(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeDouble(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readDouble(), 0, "Sequential double read should match value at index " + i);
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readDouble(i), 0, "Indexed double read should match value at index " + i);
    }

    @Test
    @DisplayName("Read and write combined double and int values")
    public void testReadWriteDoubleAndInt() {
        for (long i = 0; i < 48; i += 12)
            bytes.writeDoubleAndInt(i, (int) i);

        for (long i = 0; i < 48; i += 12) {
            assertEquals(i, bytes.readDouble(), 0, "Double part should match value at index " + i);
            assertEquals(i, bytes.readInt(), "Int part should match value at index " + i);
        }
    }

    @Test
    @DisplayName("Read and write stop-bit encoded double values")
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
            assertEquals(i, bytes.readStopBitDouble(), 0.0,
                    "Stop-bit decoded double should match written value " + i);
    }

    @Test
    @DisplayName("Stream bytes through gzip input and output")
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
                for (int i = 0; i < 12; i++)
                    bytes[i] = (byte) in.read();
                assertEquals(-1, in.read(), "Gzip input stream should reach end of stream");
                assertEquals("Hello world\n", new String(bytes, ISO_8859_1), "Decoded gzip content should match input");
            }
        } finally {
            bytes2.releaseLast();
        }
    }

    @Test
    @DisplayName("Byte streams preserve read position and values")
    public void testStream2()
            throws IOException {
        try (OutputStream out = bytes.outputStream()) {
            out.write(11);
            out.write(22);
            out.write(33);
            out.write(44);
            out.write(55);

            try (InputStream in = bytes.inputStream()) {
                assertFalse(in.markSupported(), "InputStream should not support mark/reset");
                assertEquals(11, in.read(), "First byte should be read from stream");
                assertEquals(1, bytes.readPosition(), "Read position should advance after first byte");
                assertEquals(22, in.read(), "Second byte should be read from stream");
                assertEquals(2, bytes.readPosition(), "Read position should advance after second byte");
                assertEquals(33, in.read(), "Third byte should be read from stream");

                assertEquals(1, in.skip(1), "Skip should advance by one byte");
                assertEquals(4, bytes.readPosition(), "Read position should advance after skip");
                assertEquals(1, bytes.readRemaining(), "Remaining bytes should match unread data");
                assertEquals(55, in.read(), "Final byte should be read from stream");

                assertEquals(-1, in.read(), "InputStream should report end of stream");
            }
        }
    }

    @Test
    @DisplayName("Add-and-get for int values accumulates correctly")
    public void testAddAndGet() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {
            for (int i = 0; i < 10; i++)
                bytesStore.addAndGetInt(0L, 10);
            assertEquals(100, bytesStore.readInt(0L), "Int accumulation should reach 100 at offset 0");
            assertEquals(0, bytesStore.readInt(4L), "Neighbouring offset should remain unchanged");

            for (int i = 0; i < 11; i++)
                bytesStore.addAndGetInt(4L, 11);
            assertEquals(100, bytesStore.readInt(0L), "Offset 0 value should remain 100");
            assertEquals(11 * 11, bytesStore.readInt(4L), "Offset 4 should accumulate 11 * 11");
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    @DisplayName("Add-and-get long values on native store")
    public void testAddAndGetLongNative() {
        assumeFalse(Jvm.isArm(), "ARM does not support add-and-get long test on native store");
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {
            checkAddAndGetLong();
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    @DisplayName("Add-and-get long values on heap store")
    public void testAddAndGetLong() {
        assumeFalse(Jvm.isArm(), "ARM does not support add-and-get long test on heap store");
        final BytesStore<?, ?> bytesStore2 = BytesStore.wrap(new byte[128]);
        try {
            checkAddAndGetLong();
        } finally {
            bytesStore2.releaseLast();
        }
    }

    private void checkAddAndGetLong() {
        for (int i = 0; i < 10; i++)
            assertEquals((i + 1L) * 10L, bytesStore.addAndGetLong(0L, 10),
                    "Long accumulation should increase by 10 each time at iteration " + i);
        assertEquals(100, bytesStore.readLong(0L), "Offset 0 should accumulate to 100 for long test");
        assertEquals(0, bytesStore.readLong(8L), "Offset 8 should remain unchanged for long test");

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetLong(8L, 11);
        assertEquals(100, bytesStore.readLong(0L), "Offset 0 should remain 100 after long updates");
        assertEquals(11L * 11L, bytesStore.readLong(8L), "Offset 8 should accumulate 11 * 11 for long test");
    }

    @Test
    @DisplayName("Add-and-get float values accumulates correctly")
    public void testAddAndGetFloat() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {

            for (int i = 0; i < 10; i++)
                bytesStore2.addAndGetFloat(0L, 10);
            assertEquals(100, bytesStore2.readFloat(0L), 0f, "Float accumulation should reach 100");
            assertEquals(0, bytesStore2.readVolatileFloat(4L), 0f, "Offset 4 should remain unchanged");

            for (int i = 0; i < 11; i++)
                bytesStore2.addAndGetFloat(4L, 11);
        assertEquals(100, bytesStore2.readVolatileFloat(0L), 0f, "Float offset 0 should remain 100");
        assertEquals(11 * 11, bytesStore2.readFloat(4L), 0f, "Float offset 4 should accumulate 11 * 11");
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    @DisplayName("Add-and-get double values accumulates correctly")
    public void testAddAndGetDouble() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {

            for (int i = 0; i < 10; i++)
                bytesStore2.addAndGetDouble(0L, 10);
            assertEquals(100, bytesStore2.readDouble(0L), 0.0, "Double accumulation should reach 100");
            assertEquals(0, bytesStore2.readVolatileDouble(8L), 0.0,
                    "Offset 8 should remain unchanged for double test");

            for (int i = 0; i < 11; i++)
                bytesStore2.addAndGetDouble(8L, 11);
        assertEquals(100, bytesStore2.readVolatileDouble(0L), 0.0, "Double offset 0 should remain 100");
        assertEquals(11 * 11, bytesStore2.readDouble(8L), 0.0, "Double offset 8 should accumulate 11 * 11");
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    @DisplayName("Debug string reflects read and write positions")
    public void testToString() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(), "Guarded bytes debug formatting differs on new guard");
        BytesStore<?, Void> bytes0 = BytesStore.nativeStore(32);
        final Bytes<?> bytes = bytes0.bytesForWrite();
        bytes0.release(INIT);
        try {
            assertEquals("[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Initial debug string should reflect empty positions");
            bytes.writeUnsignedByte(1);
            System.gc();
            assertEquals(1, bytes.refCount(), "Reference count should remain at one after GC");
            assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ǁ⒈‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should include first byte marker");
            bytes.writeUnsignedByte(2);
            bytes.readByte();
            assertEquals("[pos: 1, rlim: 2, wlim: 8EiB, cap: 8EiB ] ⒈ǁ⒉‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should reflect read position and second byte");
            bytes.writeUnsignedByte(3);
            assertEquals("[pos: 1, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈ǁ⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should reflect third byte without moving read position");
            bytes.writeUnsignedByte(4);
            bytes.readByte();
            assertEquals("[pos: 2, rlim: 4, wlim: 8EiB, cap: 8EiB ] ⒈⒉ǁ⒊⒋‡٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should advance read marker after another read");
            bytes.writeUnsignedByte(5);
            assertEquals("[pos: 2, rlim: 5, wlim: 8EiB, cap: 8EiB ] ⒈⒉ǁ⒊⒋⒌‡٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should include fifth byte marker");
            bytes.writeUnsignedByte(6);
            bytes.readByte();
            System.gc();
            assertEquals(1, bytes.refCount(), "Reference count should stay stable after GC");
            assertEquals("[pos: 3, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍‡٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should reflect read position at 3");
            bytes.writeUnsignedByte(7);
            assertEquals("[pos: 3, rlim: 7, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍⒎‡٠٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should include seventh byte marker");
            bytes.writeUnsignedByte(8);
            assertEquals("[pos: 3, rlim: 8, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍⒎⒏‡٠٠٠٠٠٠٠٠", bytes.toDebugString(),
                    "Debug string should include eighth byte marker");
        } finally {
            bytes.releaseLast();
            assertEquals(0, bytes.refCount(), "Reference count should reach zero after release");
        }
    }

    @Test
    @DisplayName("Read UTF-8 beyond bounds throws BufferUnderflowException")
    public void testOverflowReadUtf8()
            throws IORuntimeException {
        final BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        BytesInternal.writeStopBit(bs, 10, 30);
        try {
            BufferUnderflowException thrown = assertThrows(BufferUnderflowException.class,
                    () -> bs.readUtf8(10, new StringBuilder()),
                    "Reading past bounds should throw BufferUnderflowException");
            assertNotNull(thrown, "BufferUnderflowException should be thrown for overflow read");
        } finally {
            bs.releaseLast();
        }
    }

    @Test
    @DisplayName("CopyTo duplicates all bytes into new store")
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
                    assertEquals(bytesStoreOriginal.readByte(i), bytesStoreCopy.readByte(i),
                            "Copied byte should match source at index " + i);
            } finally {
                bytesStoreCopy.releaseLast();
            }
        } finally {
            bytesStoreOriginal.releaseLast();
        }
    }

    @Test
    @DisplayName("CopyTo respects destination offset and write position")
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
                assertEquals(SIZE - destOffset, bytesCopied, "Unexpected number of bytes copied");
                for (int i = 0; i < bytesCopied; i++)
                    assertEquals(bytesStoreOriginal.readByte(i), bytesStoreCopy.readByte(i + destOffset),
                            "Copied byte should match source at index " + i + " with dest offset");
            } finally {
                bytesStoreCopy.releaseLast();
            }
        } finally {
            bytesStoreOriginal.releaseLast();
        }
    }

    @Test
    @DisplayName("Empty BytesStore reports a zero capacity value")
    public void testEmpty() {
        assertEquals(0, BytesStore.empty().realCapacity(), "Empty BytesStore should report zero capacity");
    }

    @Test
    @DisplayName("Clear and pad beyond capacity throws overflow")
    public void testClearAndPadTooMuch() {
        final Bytes<?> b = bytesStore.bytesForWrite();
        try {
            assertThrows(DecoratedBufferOverflowException.class,
                    () -> b.clearAndPad(SIZE + 1),
                    "clearAndPad should throw when size exceeds capacity");
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("Followed direct buffer reflects written bytes")
    public void testFollow() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for follow test");
        ByteBuffer direct = ByteBuffer.allocateDirect(128);
        for (int i = 0; i < 128; i++) {
            BytesStore<?, ByteBuffer> store = BytesStore.follow(direct);
            store.write(i, new byte[] {(byte)i});
            store.releaseLast();
        }

        for (int i = 0; i < 128; i++) {
            assertEquals(i, direct.get(i), "Direct buffer byte should match value at index " + i);
        }
    }
}
