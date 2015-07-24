/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static net.openhft.chronicle.bytes.StopCharTesters.CONTROL_STOP;
import static net.openhft.chronicle.bytes.StopCharTesters.SPACE_STOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: peter.lawrey
 */
public class ByteStoreTest {
    public static final int SIZE = 128;
    private Bytes bytes;
    private ByteBuffer byteBuffer;
    private BytesStore bytesStore;

    @Before
    public void beforeTest() {
        byteBuffer = ByteBuffer.allocate(SIZE).order(ByteOrder.nativeOrder());
        bytesStore = BytesStore.wrap(byteBuffer);
        bytes = bytesStore.bytesForWrite();
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

    @Test
    public void testAppendDouble() {
        testAppendDouble0(-6.895305375646115E24);
        Random random = new Random(1);
        for (int i = 0; i < 100000; i++) {
            double d = Math.pow(1e32, random.nextDouble()) / 1e6;
            if (i % 3 == 0) d = -d;
            testAppendDouble0(d);
        }
    }

    private void testAppendDouble0(double d) {
        bytes.clear();
        bytes.append(d).append(' ');

        double d2 = bytes.parseDouble();
        Assert.assertEquals(d, d2, 0);

/* assumes self terminating.
        bytes.clear();
        bytes.append(d);
        bytes.flip();
        double d3 = bytes.parseDouble();
        Assert.assertEquals(d, d3, 0);
*/
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
    public void testWriteReadUTFΔ() {
        bytes.writeUTFΔ(null);
        String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.writeUTFΔ(word);
        }

        assertEquals(null, bytes.readUTFΔ());
        for (String word : words) {
            assertEquals(word, bytes.readUTFΔ());
        }
        try {
            bytes.readUTFΔ();
            fail();
        } catch (BufferUnderflowException e) {
            // expected
        }
        assertEquals(25, bytes.readPosition()); // check the size

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        Assert.assertFalse(bytes.readUTFΔ(sb));
        for (String word : words) {
            Assert.assertTrue(bytes.readUTFΔ(sb));
            Assert.assertEquals(word, sb.toString());
        }
        try {
            bytes.readUTFΔ(sb);
            fail();
        } catch (BufferUnderflowException e) {
            // expected
        }
        Assert.assertEquals("", sb.toString());
    }

    @Test
    public void testWriteReadUTF() {
        String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.writeUTFΔ(word);
        }
        bytes.writeUTFΔ("");
        assertEquals(24, bytes.writePosition()); // check the size, more bytes for less strings than writeUTFΔ


        for (String word : words) {
            assertEquals(word, bytes.readUTFΔ());
        }
        assertEquals("", bytes.readUTFΔ());
    }

    @Test
    public void testAppendParseUTF() {
        String[] words = "Hello,World!,Bye£€!".split(",");
        for (String word : words) {
            bytes.append(word).append('\t');
        }
        bytes.append('\t');

        for (String word : words) {
            assertEquals(word, bytes.parseUTF(CONTROL_STOP));
        }
        assertEquals("", bytes.parseUTF(CONTROL_STOP));

        bytes.readPosition(0);
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            bytes.parseUTF(sb, CONTROL_STOP);
            Assert.assertEquals(word, sb.toString());
        }
        bytes.parseUTF(sb, CONTROL_STOP);
        Assert.assertEquals("", sb.toString());

        bytes.readPosition(0);
        bytes.skipTo(CONTROL_STOP);
        assertEquals(6, bytes.readPosition());
        bytes.skipTo(CONTROL_STOP);
        assertEquals(13, bytes.readPosition());
        Assert.assertTrue(bytes.skipTo(CONTROL_STOP));
        assertEquals(23, bytes.readPosition());
        Assert.assertTrue(bytes.skipTo(CONTROL_STOP));
        assertEquals(24, bytes.readPosition());
        Assert.assertFalse(bytes.skipTo(CONTROL_STOP));
    }

    @Test
    public void testWriteReadByteBuffer() {
        byte[] bytes = "Hello\nWorld!\r\nBye".getBytes();
        this.bytes.write(ByteBuffer.wrap(bytes));

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
    public void testAppendSubstring() {
        bytes.append("Hello World", 2, 7).append("\n");

        assertEquals("Hello World".substring(2, 7), bytes.parseUTF(CONTROL_STOP));
    }

    @Test
    public void testAppendParse() {
        bytes.append("word£€)").append(' ');
        bytes.append(1234).append(' ');
        bytes.append(123456L).append(' ');
        bytes.append(1.2345).append(' ');


        assertEquals("word£€)", bytes.parseUTF(SPACE_STOP));
        assertEquals(1234, bytes.parseLong());
        assertEquals(123456L, bytes.parseLong());
        assertEquals(1.2345, bytes.parseDouble(), 0);
    }

    @Test
    public void testWriteBytes() {
        bytes.write("Hello World\n".getBytes(), 0, 10);
        bytes.write("good bye\n".getBytes(), 4, 4);
        bytes.write(4, "0 w".getBytes());

        assertEquals("Hell0 worl bye", bytes.parseUTF(CONTROL_STOP));
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
            bytesStore.getAndAddInt(4L, 11);
        assertEquals(100, bytesStore.readInt(0L));
        assertEquals(11 * 11, bytesStore.readInt(4L));
    }

    @Test
    public void testToString() {
        Bytes bytes = NativeBytesStore.nativeStore(32).bytesForWrite();
        assertEquals("[pos: 0, rlim: 0, wlim: 8EiB, cap: 8EiB ] ", bytes.toDebugString());
        bytes.writeUnsignedByte(1);
        assertEquals("[pos: 0, rlim: 1, wlim: 8EiB, cap: 8EiB ] ⒈", bytes.toDebugString());
        bytes.writeUnsignedByte(2);
        bytes.readByte();
        assertEquals("[pos: 1, rlim: 2, wlim: 8EiB, cap: 8EiB ] ⒈‖⒉", bytes.toDebugString());
        bytes.writeUnsignedByte(3);
        assertEquals("[pos: 1, rlim: 3, wlim: 8EiB, cap: 8EiB ] ⒈‖⒉⒊", bytes.toDebugString());
        bytes.writeUnsignedByte(4);
        bytes.readByte();
        assertEquals("[pos: 2, rlim: 4, wlim: 8EiB, cap: 8EiB ] ⒈⒉‖⒊⒋", bytes.toDebugString());
        bytes.writeUnsignedByte(5);
        assertEquals("[pos: 2, rlim: 5, wlim: 8EiB, cap: 8EiB ] ⒈⒉‖⒊⒋⒌", bytes.toDebugString());
        bytes.writeUnsignedByte(6);
        bytes.readByte();
        assertEquals("[pos: 3, rlim: 6, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊‖⒋⒌⒍", bytes.toDebugString());
        bytes.writeUnsignedByte(7);
        assertEquals("[pos: 3, rlim: 7, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊‖⒋⒌⒍⒎", bytes.toDebugString());
        bytes.writeUnsignedByte(8);
        assertEquals("[pos: 3, rlim: 8, wlim: 8EiB, cap: 8EiB ] ⒈⒉⒊‖⒋⒌⒍⒎⒏", bytes.toDebugString());
    }
}
