/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class StreamingDataInputTest extends BytesTestCommon {

    private final Allocator allocator;

    public StreamingDataInputTest(Allocator allocator) {
        this.allocator = allocator;
    }

    @Parameterized.Parameters(name = "allocator={0}")
    public static Object[] params() {
        return Arrays.stream(Allocator.values()).toArray();
    }

    @Test
    public void read() {
        Bytes<?> b = allocator.elasticBytes(32);
        b.append("0123456789");
        byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes();
        b.readPosition(3);
        b.read(byteArr);
        assertEquals("3456789HIJKLMNOP", new String(byteArr, StandardCharsets.ISO_8859_1));
        b.releaseLast();
    }

    @Test
    public void readOffset() {
        Bytes<?> b = allocator.elasticBytes(32);
        b.append("0123456789");
        byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes();
        b.read(byteArr, 2, 6);
        assertEquals("AB012345IJKLMNOP", new String(byteArr, StandardCharsets.ISO_8859_1));
        assertEquals('6', b.readByte());
        b.releaseLast();
    }

    @Test
    public void roundTripWorksOnHeap() {
        Bytes<?> b = allocator.elasticBytes(32);
        TestObject source = new TestObject(123L, 123, false);
        int offset = BytesUtil.triviallyCopyableStart(source.getClass());
        b.unsafeWriteObject(source, offset, 13);
        TestObject dest = new TestObject();
        b.unsafeReadObject(dest, offset, 13);
        assertEquals(source, dest);
        b.releaseLast();
    }

    @Test
    public void readWithLength() {
        int max = 130; // two bytes of length for a stop bit encoded length
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(max + 2);
        Bytes<?> from = Bytes.wrapForRead(new byte[max]);
        Bytes<?> to = Bytes.wrapForRead(new byte[max]);
        for (int len = 0; len <= max; len++) {
            from.readPositionRemaining(0, len);
            bytes.clear();
            bytes.writeWithLength(from);
            assertEquals(len + (len < 128 ? 1 : 2), bytes.readRemaining());
            bytes.readWithLength(to);
            assertEquals(len, to.readRemaining());
        }
    }

    static class TestObject {
        long l1;
        long i1;
        boolean b1;

        public TestObject() {
        }

        public TestObject(long l1, int i1, boolean b1) {
            this.l1 = l1;
            this.i1 = i1;
            this.b1 = b1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return l1 == that.l1 && i1 == that.i1 && b1 == that.b1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l1, i1, b1);
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "l1=" + l1 +
                    ", i1=" + i1 +
                    ", b1=" + b1 +
                    '}';
        }
    }
}
