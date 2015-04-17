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

import org.junit.Ignore;
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
    @Ignore("todo")
    public void testSliceOfBytes() {
        testSliceOfBytes(Bytes.wrap(new byte[1024]));
        testSliceOfBytes(Bytes.wrap(ByteBuffer.allocate(1024)));
        testSliceOfBytes(Bytes.wrap(ByteBuffer.allocateDirect(1024)));
        testSliceOfBytes(NativeBytes.nativeBytes(1024));
    }

    public static void testSliceOfBytes(Bytes bytes) {
        // move the position by 1
        bytes.readByte();

        Bytes bytes1 = bytes.bytes();
        assertEquals(bytes.capacity() - 1, bytes1.capacity());
        assertEquals(0, bytes1.position());

        // move the position by 8 more
        bytes.readLong();

        Bytes bytes9 = bytes.bytes();
        assertEquals(bytes.capacity() - 9, bytes9.capacity());
        assertEquals(0, bytes9.position());

        long num = 1234567890123456789L;
        bytes.writeLong(9, num);
        assertEquals(num, bytes1.readLong(8));
        assertEquals(num, bytes9.readLong(0));
    }
}