/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.threads.ThreadDump;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * User: peter.lawrey
 */
public class ByteStoreTest {
    public static final int SIZE = 128;
    private Bytes bytes;
    private ByteBuffer byteBuffer;
    private BytesStore bytesStore;

    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }
    @Before
    public void beforeTest() {
        byteBuffer = ByteBuffer.allocate(SIZE).order(ByteOrder.nativeOrder());
        bytesStore = BytesStore.wrap(byteBuffer);
        bytes = bytesStore.bytesForWrite();
        bytes.clear();
    }

    @Test
    public void testCopyTo() {
        final BytesStore bytesStoreOriginal = BytesStore.wrap(new byte[SIZE]);
        for(int i = 0;i<SIZE;i++) {
            final byte randomByte = (byte)ThreadLocalRandom.current().nextInt(Byte.MAX_VALUE);
            bytesStoreOriginal.writeByte(i,randomByte);
        }
        final BytesStore bytesStoreCopy = BytesStore.wrap(new byte[SIZE]);
        bytesStoreOriginal.copyTo(bytesStoreCopy);
        for (int i = 0; i < SIZE; i++)
            assertEquals(bytesStoreOriginal.readByte(i), bytesStoreCopy.readByte(i));
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
        BytesStore bytes = BytesStore.wrap(ByteBuffer.allocate(100));
        bytes.compareAndSwapLong(0, 0L, 1L);
        assertEquals(1L, bytes.readLong(0));
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

        byte[] bytes = new byte[(int) this.bytes.capacity()];
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
        assertEquals(10, Bytes.allocateDirect(10).capacity());
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
            byte[] bytes = "Hello World!".getBytes();
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
    public void testWriteReadUtf8() {
        bytes.writeUtf8(null);
        String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.writeUtf8(word);
        }

        assertEquals(null, bytes.readUtf8());
        for (String word : words) {
            assertEquals(word, bytes.readUtf8());
        }
        assertEquals(null, bytes.readUtf8());
        assertEquals(25, bytes.readPosition()); // check the size

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        Assert.assertFalse(bytes.readUtf8(sb));
        for (String word : words) {
            Assert.assertTrue(bytes.readUtf8(sb));
            Assert.assertEquals(word, sb.toString());
        }
        assertFalse(bytes.readUtf8(sb));
        Assert.assertEquals("", sb.toString());
    }

    @Test
    public void testWriteReadUTF() {
        String[] words = "Hello,World!,Bye£€!".split(",");
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
        assertEquals(null, bytes.readUtf8());
    }

    @Test
    public void testWriteReadByteBuffer() {
        byte[] bytes = "Hello\nWorld!\r\nBye".getBytes();
        this.bytes.writeSome(ByteBuffer.wrap(bytes));

        byte[] bytes2 = new byte[bytes.length + 1];
        ByteBuffer bb2 = ByteBuffer.wrap(bytes2);
        this.bytes.read(bb2);

        Assert.assertEquals(bytes.length, bb2.position());
        byte[] bytes2b = Arrays.copyOf(bytes2, bytes.length);
        Assert.assertTrue(Arrays.equals(bytes, bytes2b));
    }

    @Test
    public void testReadWriteBoolean() {
        for (int i = 0; i < 32; i++)
            bytes.writeBoolean(i, (i & 3) == 0);
        bytes.writePosition(32);
        for (int i = 32; i < 64; i++)
            bytes.writeBoolean((i & 5) == 0);

        for (int i = 0; i < 32; i++)
            assertEquals((i & 3) == 0, bytes.readBoolean());
        for (int i = 32; i < 64; i++)
            assertEquals((i & 5) == 0, bytes.readBoolean(i));
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
    public void testReadWriteStop() {
        long[] longs = {Long.MIN_VALUE, Long.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (long i : longs) {
            bytes.writeStopBit(i);
//            LOG.info(i + " " + bytes.position());
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
    public void testReadWriteThreadeSafeInt() {
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
    public void testReadWriteUnsignedInt() {
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
        for (long i = 0; i < 32; i += 8)
            bytes.writeOrderedLong(i, i);
        bytes.writePosition(32);
        for (long i = 32; i < 64; i += 8)
            bytes.writeOrderedLong(i);
//        LOG.info(bytes.bytes().toDebugString());

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
    public void testReadWriteStopBitDouble() {
        double[] doubles = {
                -Double.MAX_VALUE, Double.NEGATIVE_INFINITY,
                Byte.MIN_VALUE, Byte.MAX_VALUE,
                Short.MIN_VALUE, Short.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (double i : doubles) {
            bytes.writeStopBit(i);
            System.out.println(i + " " + bytes.writePosition());
        }

        for (double i : doubles)
            assertEquals(i, bytes.readStopBitDouble(), 0.0);
    }

    @Test
    @Ignore
    public void testStream() throws IOException {
        bytes = BytesStore.wrap(ByteBuffer.allocate(1000)).bytesForWrite();
        GZIPOutputStream out = new GZIPOutputStream(bytes.outputStream());
        out.write("Hello world\n".getBytes());
        out.close();

        GZIPInputStream in = new GZIPInputStream(bytes.inputStream());
        byte[] bytes = new byte[12];
        for (int i = 0; i < 12; i++)
            bytes[i] = (byte) in.read();
        Assert.assertEquals(-1, in.read());
        Assert.assertEquals("Hello world\n", new String(bytes));
        in.close();
    }

    @Test
    @Ignore
    public void testStream2() throws IOException {
        OutputStream out = bytes.outputStream();
        out.write(11);
        out.write(22);
        out.write(33);
        out.write(44);
        out.write(55);

        InputStream in = bytes.inputStream();
        Assert.assertTrue(in.markSupported());
        Assert.assertEquals(11, in.read());
        in.mark(1);
        assertEquals(1, bytes.readPosition());
        Assert.assertEquals(22, in.read());
        assertEquals(2, bytes.readPosition());

        Assert.assertEquals(33, in.read());
        in.reset();

        assertEquals(1, bytes.readPosition());
        Assert.assertEquals(22, in.read());

        Assert.assertEquals(2, in.skip(2));
        assertEquals(4, bytes.readPosition());
        assertEquals(SIZE - 4, bytes.readRemaining());
        Assert.assertEquals(55, in.read());
        in.close();
    }

    @Test
    public void testAddAndGet() {
        bytesStore = NativeBytesStore.nativeStore(128);

        for (int i = 0; i < 10; i++)
            bytesStore.addAndGetInt(0L, 10);
        assertEquals(100, bytesStore.readInt(0L));
        assertEquals(0, bytesStore.readInt(4L));

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetInt(4L, 11);
        assertEquals(100, bytesStore.readInt(0L));
        assertEquals(11 * 11, bytesStore.readInt(4L));
    }

    @Test
    public void testAddAndGetLong() {
        bytesStore = NativeBytesStore.nativeStore(128);

        for (int i = 0; i < 10; i++)
            bytesStore.addAndGetLong(0L, 10);
        assertEquals(100, bytesStore.readLong(0L));
        assertEquals(0, bytesStore.readLong(8L));

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetLong(8L, 11);
        assertEquals(100, bytesStore.readLong(0L));
        assertEquals(11 * 11, bytesStore.readLong(8L));
    }

    @Test
    public void testAddAndGetFloat() {
        bytesStore = NativeBytesStore.nativeStore(128);

        for (int i = 0; i < 10; i++)
            bytesStore.addAndGetFloat(0L, 10);
        assertEquals(100, bytesStore.readFloat(0L), 0f);
        assertEquals(0, bytesStore.readVolatileFloat(4L), 0f);

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetFloat(4L, 11);
        assertEquals(100, bytesStore.readVolatileFloat(0L), 0f);
        assertEquals(11 * 11, bytesStore.readFloat(4L), 0f);
    }

    @Test
    public void testAddAndGetDouble() {
        bytesStore = NativeBytesStore.nativeStore(128);

        for (int i = 0; i < 10; i++)
            bytesStore.addAndGetDouble(0L, 10);
        assertEquals(100, bytesStore.readDouble(0L), 0.0);
        assertEquals(0, bytesStore.readVolatileDouble(8L), 0.0);

        for (int i = 0; i < 11; i++)
            bytesStore.addAndGetDouble(8L, 11);
        assertEquals(100, bytesStore.readVolatileDouble(0L), 0.0);
        assertEquals(11 * 11, bytesStore.readDouble(8L), 0.0);
    }

    @Test
    public void testToString() {
        Bytes bytes = NativeBytesStore.nativeStore(32).bytesForWrite();
        assertEquals("[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ‖‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(1);
        assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ‖⒈‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(2);
        bytes.readByte();
        assertEquals("[pos: 1, rlim: 2, wlim: 8EiB, cap: 8EiB ] ⒈‖⒉‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(3);
        assertEquals("[pos: 1, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈‖⒉⒊‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(4);
        bytes.readByte();
        assertEquals("[pos: 2, rlim: 4, wlim: 8EiB, cap: 8EiB ] ⒈⒉‖⒊⒋‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(5);
        assertEquals("[pos: 2, rlim: 5, wlim: 8EiB, cap: 8EiB ] ⒈⒉‖⒊⒋⒌‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(6);
        bytes.readByte();
        assertEquals("[pos: 3, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊‖⒋⒌⒍‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(7);
        assertEquals("[pos: 3, rlim: 7, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊‖⒋⒌⒍⒎‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
        bytes.writeUnsignedByte(8);
        assertEquals("[pos: 3, rlim: 8, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊‖⒋⒌⒍⒎⒏‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", bytes.toDebugString());
    }

    @Test
    public void testOverflowReadUtf8() {
        NativeBytesStore<Void> bs = NativeBytesStore.nativeStore(32);
        BytesInternal.writeStopBit(bs, 10, 30);
        try {
            bs.readUtf8(10, new StringBuilder());
            throw new AssertionError("should throw BufferUnderflowException");
        } catch (BufferUnderflowException e) {
            // expected
        }
    }
}
