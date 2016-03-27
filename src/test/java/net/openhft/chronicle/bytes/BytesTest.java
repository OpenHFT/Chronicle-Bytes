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

import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class BytesTest {
    /*
        public static void testSliceOfBytes(Bytes bytes) {
            // move the position by 1
            bytes.readByte();
            // and reduce the limit
            long limit1 = bytes.readLimit() - 1;
            bytes.limit(limit1);

            Bytes bytes1 = bytes.bytes();
            assertFalse(bytes1.isElastic());
            assertEquals(1, bytes1.start());
            assertEquals(limit1, bytes1.capacity());
            assertEquals(bytes1.limit(), bytes1.capacity());
            assertEquals(1, bytes1.position());

            // move the position by 8 more
            bytes1.readLong();
            // reduce the limit by 8
            long limit9 = bytes1.limit() - 8;
            bytes1.limit(limit9);

            Bytes bytes9 = bytes1.bytes();
            assertEquals(1 + 8, bytes9.start());
            assertEquals(limit9, bytes9.capacity());
            assertEquals(bytes9.limit(), bytes9.capacity());
            assertEquals(9, bytes9.position());

            long num = 0x0123456789ABCDEFL;
            bytes.writeLong(9, num);

            long num1 = bytes1.readLong(bytes1.start() + 8);
            assertEquals(Long.toHexString(num1), num, num1);
            long num9 = bytes9.readLong(bytes9.start());
            assertEquals(Long.toHexString(num9), num, num9);
        }

        public static void testSliceOfZeroedBytes(Bytes bytes) {
            // move the position by 1
            bytes.readByte();
            // and reduce the limit
            bytes.limit(bytes.limit() - 1);

            Bytes bytes1 = bytes.bytes();
            assertFalse(bytes1.isElastic());

            assertEquals(1, bytes1.start());
            // capacity is notional in this case.
    //        assertEquals(bytes.capacity() - 1, bytes1.capacity());
            assertEquals(bytes1.limit(), bytes1.capacity());
            assertEquals(1, bytes1.position());

            // move the position by 8 more
            bytes1.readLong();
            // reduce the limit by 8
            bytes1.limit(bytes1.limit() - 8);

            Bytes bytes9 = bytes1.bytes();
            assertEquals(1 + 8, bytes9.start());
    //        assertEquals(bytes1.capacity() - 8 - 8, bytes9.capacity());
            assertEquals(bytes9.limit(), bytes9.capacity());
            assertEquals(9, bytes9.position());

            long num = 0x0123456789ABCDEFL;
            bytes.writeLong(9, num);

            long num1 = bytes1.readLong(bytes1.start() + 8);
            assertEquals(Long.toHexString(num1), num, num1);
            long num9 = bytes9.readLong(bytes9.start());
            assertEquals(Long.toHexString(num9), num, num9);
        }
    */
    @Test
    public void testName() throws IORuntimeException {
        Bytes<Void> bytes = Bytes.allocateDirect(30);

        long expected = 12345L;
        int offset = 5;

        bytes.writeLong(offset, expected);
        bytes.writePosition(offset + 8);
        assertEquals(expected, bytes.readLong(offset));
    }

    /*

        @Test
        public void testSliceOfBytes() {
            testSliceOfBytes(Bytes.wrap(new byte[1024]));
            testSliceOfBytes(Bytes.wrap(ByteBuffer.allocate(1024)));
            testSliceOfBytes(Bytes.wrap(ByteBuffer.allocateDirect(1024)));
        }

        @Test
        public void testSliceOfZeroedBytes() {
            testSliceOfZeroedBytes(NativeBytes.vanillaBytes(1024));
        }
    */
    @Test
    public void testCopy() {
        Bytes<ByteBuffer> bbb = Bytes.wrapForWrite(ByteBuffer.allocateDirect(1024));
        for (int i = 'a'; i <= 'z'; i++)
            bbb.writeUnsignedByte(i);
        bbb.readPositionRemaining(4, 12);
        BytesStore<Bytes<ByteBuffer>, ByteBuffer> copy = bbb.copy();
        bbb.writeUnsignedByte(10, '0');
        assertEquals("[pos: 0, rlim: 12, wlim: 12, cap: 12 ] efghijklmnop", copy.toDebugString());
    }

    @Test
    public void toHexString() throws IOException {
        Bytes bytes = Bytes.allocateElasticDirect(1020);
        bytes.append("Hello World");
        assertEquals("00000000 48 65 6C 6C 6F 20 57 6F  72 6C 64                Hello Wo rld     \n", bytes.toHexString());
        bytes.readLimit(bytes.realCapacity());
        assertEquals("00000000 48 65 6C 6C 6F 20 57 6F  72 6C 64 00 00 00 00 00 Hello Wo rld·····\n" +
                "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                "........\n" +
                "000003f0 00 00 00 00 00 00 00 00  00 00 00 00             ········ ····    \n", bytes.toHexString());

        assertEquals("00000000 48 65 6C 6C 6F 20 57 6F  72 6C 64 00 00 00 00 00 Hello Wo rld·····\n" +
                "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                "........\n" +
                "000000f0 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                "... truncated", bytes.toHexString(256));
    }

    @Test
    public void fromHexString() {
        Bytes bytes = Bytes.elasticByteBuffer();
        for (int i = 0; i < 259; i++)
            bytes.writeByte((byte) i);
        String s = bytes.toHexString();
        Bytes bytes2 = Bytes.fromHexString(s);
        assertEquals(s, bytes2.toHexString());
    }

    @Test
    public void testCharAt() {
        Bytes b = Bytes.from("Hello World");
        b.readSkip(6);
        assertTrue(StringUtils.isEqual("World", b));
    }

    @Test
    public void internBytes() {
        Bytes b = Bytes.from("Hello World");
        b.readSkip(6);
        {
            StringInterner si = new StringInterner(128);
            String s = si.intern(b);
            String s2 = si.intern(b);
            assertEquals("World", s);
            assertSame(s, s2);
        }
        {
            UTF8StringInterner si = new UTF8StringInterner(128);
            String s = si.intern(b);
            String s2 = si.intern(b);
            assertEquals("World", s);
            assertSame(s, s2);
        }
    }

    @Test
    public void testStopBitDouble() {
        Bytes b = Bytes.elasticByteBuffer();
        testSBD(b, -0.0, "00000000 40                                               @                \n");
        testSBD(b, -1.0, "00000000 DF 7C                                            ·|               \n");
        testSBD(b, -12345678, "00000000 E0 D9 F1 C2 4E                                   ····N            \n");
        testSBD(b, 0.0, "00000000 00                                               ·                \n");
        testSBD(b, 1.0, "00000000 9F 7C                                            ·|               \n");
        testSBD(b, 1024, "00000000 A0 24                                            ·$               \n");
        testSBD(b, 1000000, "00000000 A0 CB D0 48                                      ···H             \n");
        testSBD(b, 0.1, "00000000 9F EE B3 99 CC E6 B3 99  4D                      ········ M       \n");
        testSBD(b, Double.NaN, "00000000 BF 7E                                            ·~               \n");
    }

    private void testSBD(Bytes b, double v, String s) {
        b.clear();
        b.writeStopBit(v);
        assertEquals(s, b.toHexString());
    }

    @Test
    public void testOneRelease() {
        int count = 0;
        for (Bytes b : new Bytes[]{
                Bytes.allocateDirect(10),
                Bytes.allocateDirect(new byte[5]),
                Bytes.allocateElasticDirect(100),
                Bytes.elasticByteBuffer(),
                Bytes.wrapForRead(new byte[1]),
                Bytes.wrapForRead(ByteBuffer.allocateDirect(128)),
                Bytes.wrapForWrite(new byte[1]),
                Bytes.wrapForWrite(ByteBuffer.allocateDirect(128)),
        }) {
            assertEquals(count + ": " + b.getClass().getSimpleName(), 1, b.refCount());
            assertEquals(count + ": " + b.getClass().getSimpleName(), 1, b.bytesStore().refCount());

            b.close();
            assertEquals(count + ": " + b.getClass().getSimpleName(), 0, b.refCount());
            assertEquals(count++ + ": " + b.getClass().getSimpleName(), 0, b.bytesStore().refCount());
        }

//        Bytes.allocateElasticDirect(),
    }

    @Test
    public void testParseUtf8() {
        Bytes bytes = Bytes.allocateElasticDirect();
        bytes.appendUtf8("starting Hello World");
        String s0 = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
        assertEquals("starting", s0);
        String s = bytes.parseUtf8(StopCharTesters.ALL);
        assertEquals("Hello World", s);
    }

    @Test
    public void testPartialWrite() {
        Bytes from = Bytes.elasticByteBuffer();
        from.write("Hello World");
        Bytes to = Bytes.wrapForWrite(ByteBuffer.allocateDirect(6));

        to.writeSome(from);
        assertEquals("World", from.toString());
    }

    @Test(expected = BufferOverflowException.class)
    public void testPartialWriteArray() {
        byte[] array = "Hello World".getBytes();
        Bytes to = Bytes.wrapForWrite(ByteBuffer.allocateDirect(6));
        to.write(array);
    }

    @Test
    public void testPartialWriteBB() {
        ByteBuffer bb = ByteBuffer.wrap("Hello World".getBytes());
        Bytes to = Bytes.wrapForWrite(ByteBuffer.allocateDirect(6));

        to.writeSome(bb);
        assertEquals("World", Bytes.wrapForRead(bb).toString());
    }

    @Test
    public void testPartialWrite64plus() {
        Bytes from = Bytes.elasticByteBuffer();
        from.write("Hello World 0123456789012345678901234567890123456789012345678901234567890123456789");
        Bytes to = Bytes.wrapForWrite(ByteBuffer.allocateDirect(6));

        to.writeSome(from);
        assertTrue("from: " + from, from.toString().startsWith("World "));
    }

    @Test
    public void testCompact() {
        Bytes from = Bytes.elasticByteBuffer();
        from.write("Hello World");
        from.readLong();
        from.compact();
        assertEquals("rld", from.toString());
        assertEquals(0, from.readPosition());
    }

    @Test
    public void testParseToBytes() {
        Bytes from = Bytes.allocateDirect(64);
        from.append8bit("0123456789 aaaaaaaaaa 0123456789 0123456789");
        Bytes to = Bytes.allocateDirect(32);
        for (int i = 0; i < 4; i++) {
            from.parse8bit(to, StopCharTesters.SPACE_STOP);
            assertEquals(10, to.readRemaining());
        }
        assertEquals(0, from.readRemaining());

    }
}