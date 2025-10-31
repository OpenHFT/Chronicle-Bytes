/*
 * Copyright 2016-2025 chronicle.software
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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.*;

public class BytesBoundsAndLimitsTest extends BytesTestCommon {

    @Test(expected = BufferOverflowException.class)
    public void writeBeyondWriteLimitThrows() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            b.writeLimit(4);
            b.writeLong(1L); // 8 bytes > writeLimit
        } finally {
            b.releaseLast();
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void readBeyondReadLimitThrows() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(8);
        try {
            b.writeInt(123);
            b.readPosition(0);
            b.readLong(); // 8 bytes > available 4
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void clearIsClearAndZeroOut() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(16);
        try {
            assertTrue(b.isClear());
            b.writeLimit(b.capacity() - 1);
            assertFalse(b.isClear());
            b.clear();
            assertTrue(b.isClear());

            b.append("abcdef");
            b.zeroOut(0, 6);
            for (int i = 0; i < 6; i++) {
                assertEquals(0, b.peekUnsignedByte(i));
            }
        } finally {
            b.releaseLast();
        }
    }
}
