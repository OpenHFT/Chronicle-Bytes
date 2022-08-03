/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class ByteStoreTest extends BytesTestCommon {

    private static final int SIZE = 128;
    private Bytes<?> bytes;
    private BytesStore<?, ?> bytesStore;

    @Override
    public void afterChecks() {
        bytes.releaseLast();
        super.afterChecks();
    }

    @Before
    public void beforeTest() {
        bytesStore = BytesStore.wrap(ByteBuffer.allocate(SIZE).order(ByteOrder.nativeOrder()));
        bytes = bytesStore.bytesForWrite();
        bytesStore.release(INIT);
        bytes.clear();
    }

    @Test
    public void testReadIncompleteLong() {
        bytes.writeLong(0x0102030405060708L);
        assertEquals(0x0102030405060708L, bytes.readIncompleteLong(0));
        bytes.clear();

        long l = 0;
        for (int i = 1; i <= 8; i++) {
            bytes.writeUnsignedByte(i);
            l |= (long) i << (i * 8 - 8);
            assertEquals(l, bytes.readIncompleteLong(0));
        }
    }

    @Test
    public void testCAS() {
        if (Jvm.isArm()) return; // TODO FIX
        final BytesStore<?, ?> bytes = BytesStore.wrap(ByteBuffer.allocate(100));
        bytes.compareAndSwapLong(0, 0L, 1L);
        assertEquals(1L, bytes.readLong(0));
        bytes.releaseLast();
    }

    @Test
    public void testRead() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte(i, i);
        bytes.writePosition(bytes.capacity());
        for (int i = 0; i < bytes.capacity(); i++)
            assertEquals((byte) i, bytes.readByte());
        for (int i = (int) (bytes.capacity() - 1); i >= 0; i--) {
            assertEquals((byte) i, bytes.readByte(i));
        }
    }

    @Test
    public void testReadFully() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte((byte) i);

        @NotNull byte[] bytes = new byte[(int) this.bytes.capacity()];
        this.bytes.read(bytes);
        for (int i = 0; i < this.bytes.capacity(); i++)
            Assert.assertEquals((byte) i, bytes[i]);
    }

    @Test
    public void testCompareAndSetInt() {
        Assert.assertTrue(bytes.compareAndSwapInt(0, 0, 1));
        Assert.assertFalse(bytes.compareAndSwapInt(0, 0, 1));
        Assert.assertTrue(bytes.compareAndSwapInt(8, 0, 1));
        Assert.assertTrue(bytes.compareAndSwapInt(0, 1, 2));
    }

    @Test
    public void testCompareAndSetLong() {
        if (Jvm.isArm()) return; // TODO FIX

        Assert.assertTrue(bytes.compareAndSwapLong(0L, 0L, 1L));
        Assert.assertFalse(bytes.compareAndSwapLong(0L, 0L, 1L));
        Assert.assertTrue(bytes.compareAndSwapLong(8L, 0L, 1L));
        Assert.assertTrue(bytes.compareAndSwapLong(0L, 1L, 2L));
    }

    @Test
    public void testPosition() {
        for (int i = 0; i < bytes.capacity(); i++)
            bytes.writeByte((byte) i);
        for (int i = (int) (bytes.capacity() - 1); i >= 0; i--) {
            bytes.readPosition(i);
            assertEquals((byte) i, bytes.readByte());
        }
    }

    @Test
    public void testCapacity() {
        assertEquals(SIZE, bytes.capacity());
        final VanillaBytes<Void> bytes = Bytes.allocateDirect(10);
        try {
            assertEquals(10, bytes.capacity());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void testRemaining() {
        assertEquals(0, bytes.readRemaining());
        assertEquals(SIZE, bytes.writeRemaining());
        bytes.writePosition(10);
        assertEquals(10, bytes.readRemaining());
        assertEquals(SIZE - 10, bytes.writeRemaining());
    }

    @Test
    public void testByteOrder() {
        assertEquals(ByteOrder.nativeOrder(), bytes.byteOrder());
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
        final String[] words = new String[]{"Hello", "World!", "Bye£€!", ""};
        for (String word : words) {
            bytes.writeUtf8(word);
        }

        assertNull(bytes.readUtf8());
        for (String word : words) {
            assertEquals(word, bytes.readUtf8());
        }
        assertEquals("", bytes.readUtf8());
        assertEquals(26, bytes.readPosition()); // check the size

        bytes.readPosition(0);
        final StringBuilder sb = new StringBuilder();
        Assert.assertFalse(bytes.readUtf8(sb));
        for (String word : words) {
            Assert.assertTrue(bytes.readUtf8(sb));
            Assert.assertEquals(word, sb.toString());
        }
        assertTrue(bytes.readUtf8(sb));
        Assert.assertEquals("", sb.toString());
    }

    @Test
    public void testWriteReadUTF() {
        final String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.writeUtf8(word);
        }
        bytes.writeUtf8("");
        bytes.writeUtf8(null);
        assertEquals(26, bytes.writePosition()); // check the size, more bytes for less strings than writeUtf8

        for (String word : words) {
            assertEquals(word, bytes.readUtf8());
        }
        assertEquals("", bytes.readUtf8());
        assertNull(bytes.readUtf8());
    }

    @Test
    public void testWriteReadByteBuffer() {
        final byte[] bytes = "Hello\nWorld!\r\nBye".getBytes(ISO_8859_1);
        this.bytes.writeSome(ByteBuffer.wrap(bytes));

        final byte[] bytes2 = new byte[bytes.length + 1];
        final ByteBuffer bb2 = ByteBuffer.wrap(bytes2);
        this.bytes.read(bb2);

        Assert.assertEquals(bytes.length, bb2.position());
        final byte[] bytes2b = Arrays.copyOf(bytes2, bytes.length);
        Assert.assertArrayEquals(bytes, bytes2b);
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
            assertEquals((i & 3) == 0, bytes.readBoolean());
        for (int i = 32; i < 64; i++) {
            final boolean actual = bytes.readBoolean(i);
            final boolean expected = (i & 5) == 0;
            assertEquals(expected, actual);
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
            assertEquals(i, bytes.readShort());
        for (int i = 32; i < 64; i += 2)
            assertEquals(i, bytes.readShort(i));
    }

    @Test
    public void testReadWriteStop()
            throws IORuntimeException {
        final long[] longs = {Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (long i : longs) {
            bytes.writeStopBit(i);
//            Jvm.debug().on(getClass(), i + " " + bytes.position());
        }
        assertEquals(9 + 10, +5 + 6, bytes.writePosition());

        for (long i : longs)
            assertEquals(i, bytes.readStopBit());
    }

    @Test
    public void testReadWriteUnsignedShort() {
        for (int i = 0; i < 32; i += 2)
            bytes.writeUnsignedShort(i, (~i) & 0xFFFF);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 2)
            bytes.writeUnsignedShort(~i & 0xFFFF);

        for (int i = 0; i < 32; i += 2)
            assertEquals(~i & 0xFFFF, bytes.readUnsignedShort());
        for (int i = 32; i < 64; i += 2)
            assertEquals(~i & 0xFFFF, bytes.readUnsignedShort(i));
    }

    @Test
    public void testReadWriteInt() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeInt(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeInt(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readInt());
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readInt(i));
    }

    @Test
    public void testReadWriteThreadSafeInt() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeOrderedInt(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeOrderedInt(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readVolatileInt());
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readVolatileInt(i));
    }

    @Test
    public void testReadWriteFloat() {
        for (int i = 0; i < 32; i += 4)
            bytes.writeFloat(i, i);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i += 4)
            bytes.writeFloat(i);

        for (int i = 0; i < 32; i += 4)
            assertEquals(i, bytes.readFloat(), 0);
        for (int i = 32; i < 64; i += 4)
            assertEquals(i, bytes.readFloat(i), 0);
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
            assertEquals(~i & 0xFFFFL, bytes.readUnsignedInt());
        for (int i = 32; i < 64; i += 4)
            assertEquals(~i & 0xFFFFL, bytes.readUnsignedInt(i));
    }

    @Test
    public void testReadWriteLong() {
        for (long i = 0; i < 32; i += 8)
            bytes.writeLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeLong(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readLong());
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readLong(i));
    }

    @Test
    public void testReadWriteThreadSafeLong() {
        if (Jvm.isArm())
            return; // TODO FIX
        for (long i = 0; i < 32; i += 8)
            bytes.writeOrderedLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeOrderedLong(i);
//        Jvm.debug().on(getClass(), bytes.bytes().toDebugString());

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readVolatileLong());
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readVolatileLong(i));
    }

    @Test
    public void testReadWriteDouble() {
        for (long i = 0; i < 32; i += 8)
            bytes.writeDouble(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeDouble(i);

        for (long i = 0; i < 32; i += 8)
            assertEquals(i, bytes.readDouble(), 0);
        for (long i = 32; i < 64; i += 8)
            assertEquals(i, bytes.readDouble(i), 0);
    }

    @Test
    public void testReadWriteDoubleAndInt() {
        for (long i = 0; i < 48; i += 12)
            bytes.writeDoubleAndInt(i, (int) i);

        for (long i = 0; i < 48; i += 12) {
            assertEquals(i, bytes.readDouble(), 0);
            assertEquals(i, bytes.readInt());
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
            assertEquals(i, bytes.readStopBitDouble(), 0.0);
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
                for (int i = 0; i < 12; i++)
                    bytes[i] = (byte) in.read();
                Assert.assertEquals(-1, in.read());
                Assert.assertEquals("Hello world\n", new String(bytes));
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
                assertFalse(in.markSupported());
                assertEquals(11, in.read());
                assertEquals(1, bytes.readPosition());
                assertEquals(22, in.read());
                assertEquals(2, bytes.readPosition());
                assertEquals(33, in.read());

                assertEquals(1, in.skip(1));
                assertEquals(4, bytes.readPosition());
                assertEquals(1, bytes.readRemaining());
                assertEquals(55, in.read());

                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public void testAddAndGet() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {
            for (int i = 0; i < 10; i++)
                bytesStore.addAndGetInt(0L, 10);
            assertEquals(100, bytesStore.readInt(0L));
            assertEquals(0, bytesStore.readInt(4L));

            for (int i = 0; i < 11; i++)
                bytesStore.addAndGetInt(4L, 11);
            assertEquals(100, bytesStore.readInt(0L));
            assertEquals(11 * 11, bytesStore.readInt(4L));
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testAddAndGetLongNative() {
        if (Jvm.isArm())
            return; // TODO FIX
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {
            checkAddAndGetLong();
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testAddAndGetLong() {
        if (Jvm.isArm())
            return; // TODO FIX
        final BytesStore<?, ?> bytesStore2 = BytesStore.wrap(new byte[128]);
        try {
            checkAddAndGetLong();
        } finally {
            bytesStore2.releaseLast();
        }
    }

    private void checkAddAndGetLong() {
        for (int i = 0; i < 10; i++)
            assertEquals((i + 1) * 10, bytesStore.addAndGetLong(0L, 10));
        assertEquals(100, bytesStore.readLong(0L));
        assertEquals(0, bytesStore.readLong(8L));

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetLong(8L, 11);
        assertEquals(100, bytesStore.readLong(0L));
        assertEquals(11 * 11, bytesStore.readLong(8L));
    }

    @Test
    public void testAddAndGetFloat() {
        final BytesStore<?, ?> bytesStore2 = BytesStore.nativeStore(128);
        try {

            for (int i = 0; i < 10; i++)
                bytesStore2.addAndGetFloat(0L, 10);
            assertEquals(100, bytesStore2.readFloat(0L), 0f);
            assertEquals(0, bytesStore2.readVolatileFloat(4L), 0f);

            for (int i = 0; i < 11; i++)
                bytesStore2.addAndGetFloat(4L, 11);
            assertEquals(100, bytesStore2.readVolatileFloat(0L), 0f);
            assertEquals(11 * 11, bytesStore2.readFloat(4L), 0f);
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
            assertEquals(100, bytesStore2.readDouble(0L), 0.0);
            assertEquals(0, bytesStore2.readVolatileDouble(8L), 0.0);

            for (int i = 0; i < 11; i++)
                bytesStore2.addAndGetDouble(8L, 11);
            assertEquals(100, bytesStore2.readVolatileDouble(0L), 0.0);
            assertEquals(11 * 11, bytesStore2.readDouble(8L), 0.0);
        } finally {
            bytesStore2.releaseLast();
        }
    }

    @Test
    public void testToString() {
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        BytesStore bytes0 = BytesStore.nativeStore(32);
        final Bytes<?> bytes = bytes0.bytesForWrite();
        bytes0.release(INIT);
        try {
            assertEquals("[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ǁ‡٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(1);
            System.gc();
            assertEquals(1, bytes.refCount());
            assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ǁ⒈‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(2);
            bytes.readByte();
            assertEquals("[pos: 1, rlim: 2, wlim: 8EiB, cap: 8EiB ] ⒈ǁ⒉‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(3);
            assertEquals("[pos: 1, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈ǁ⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(4);
            bytes.readByte();
            assertEquals("[pos: 2, rlim: 4, wlim: 8EiB, cap: 8EiB ] ⒈⒉ǁ⒊⒋‡٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(5);
            assertEquals("[pos: 2, rlim: 5, wlim: 8EiB, cap: 8EiB ] ⒈⒉ǁ⒊⒋⒌‡٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(6);
            bytes.readByte();
            System.gc();
            assertEquals(1, bytes.refCount());
            assertEquals("[pos: 3, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍‡٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(7);
            assertEquals("[pos: 3, rlim: 7, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍⒎‡٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
            bytes.writeUnsignedByte(8);
            assertEquals("[pos: 3, rlim: 8, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊ǁ⒋⒌⒍⒎⒏‡٠٠٠٠٠٠٠٠", bytes.toDebugString());
        } finally {
            bytes.releaseLast();
            assertEquals(0, bytes.refCount());
        }
    }

    @Test
    public void testOverflowReadUtf8()
            throws IORuntimeException {
        final BytesStore<?, ?> bs = BytesStore.nativeStore(32);
        BytesInternal.writeStopBit(bs, 10, 30);
        try {
            bs.readUtf8(10, new StringBuilder());
            fail("should throw BufferUnderflowException");
        } catch (BufferUnderflowException e) {
            // expected
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
                    assertEquals(bytesStoreOriginal.readByte(i), bytesStoreCopy.readByte(i));
            } finally {
                bytesStoreCopy.releaseLast();
            }
        } finally {
            bytesStoreOriginal.releaseLast();
        }
    }

    @Test
    public void testEmpty() {
        assertEquals(0, BytesStore.empty().realCapacity());
    }

    @Test(expected = DecoratedBufferOverflowException.class)
    public void testClearAndPadTooMuch() {
        final Bytes<?> b = bytesStore.bytesForWrite();
        try {
            b.clearAndPad(SIZE + 1);
        } finally {
            b.releaseLast();
        }
    }
}
