/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class BytesTest {


    @Test
    public void testName() throws Exception {
        NativeBytesStore<Void> nativeStore = NativeBytesStore.nativeStoreWithFixedCapacity(30);
        Bytes<Void> bytes = nativeStore.bytes();

        long expected = 12345L;
        int offset = 5;

        bytes.writeLong(offset, expected);
        assertEquals(expected, bytes.readLong(offset));
    }

    @Test
    public void testSliceOfBytes() {
        testSliceOfBytes(Bytes.wrap(new byte[1024]));
        testSliceOfBytes(Bytes.wrap(ByteBuffer.allocate(1024)));
        testSliceOfBytes(Bytes.wrap(ByteBuffer.allocateDirect(1024)));
    }

    public static void testSliceOfBytes(Bytes bytes) {
        // move the position by 1
        bytes.readByte();
        // and reduce the limit
        bytes.limit(bytes.limit() - 1);

        Bytes bytes1 = bytes.bytes();
        assertEquals(1, bytes1.start());
        assertEquals(bytes.capacity() - 1 - 1, bytes1.capacity());
        assertEquals(bytes1.limit(), bytes1.capacity());
        assertEquals(1, bytes1.position());

        // move the position by 8 more
        bytes1.readLong();
        // reduce the limit by 8
        bytes1.limit(bytes1.limit() - 8);

        Bytes bytes9 = bytes1.bytes();
        assertEquals(1 + 8, bytes9.start());
        assertEquals(bytes1.capacity() - 8 - 8, bytes9.capacity());
        assertEquals(bytes9.limit(), bytes9.capacity());
        assertEquals(9, bytes9.position());

        long num = 0x0123456789ABCDEFL;
        bytes.writeLong(9, num);

        long num1 = bytes1.readLong(bytes1.start() + 8);
        assertEquals(Long.toHexString(num1), num, num1);
        long num9 = bytes9.readLong(bytes9.start());
        assertEquals(Long.toHexString(num9), num, num9);
    }

    @Test
    public void testSliceOfZeroedBytes() {
        testSliceOfZeroedBytes(NativeBytes.nativeBytes(1024));
    }

    public static void testSliceOfZeroedBytes(Bytes bytes) {
        // move the position by 1
        bytes.readByte();
        // and reduce the limit
        bytes.limit(bytes.limit() - 1);

        Bytes bytes1 = bytes.bytes();
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
}