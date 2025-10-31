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

import static org.junit.Assert.*;

public class BytesWriteSkipBehaviourTest extends BytesTestCommon {

    @Test
    public void reserveThenFillHeader() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            long start = bytes.writePosition();
            bytes.writeSkip(4); // reserve header
            bytes.writeInt(0x11223344);
            bytes.writeShort((short) 0x55AA);
            long end = bytes.writePosition();
            // backfill header with payload length
            long payloadLen = end - start - 4;
            bytes.writeInt(start, (int) payloadLen);

            bytes.readPosition(start);
            assertEquals(payloadLen, bytes.readInt());
            assertEquals(0x11223344, bytes.readInt());
            assertEquals((short) 0x55AA, bytes.readShort());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void backtrackOneRemovesTrailingSeparator() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            // Use length-prefixed UTF-8 so readUtf8() is valid
            bytes.writeUtf8("abc,");
            // Overwrite the last payload byte (comma) with 'd'
            bytes.writeSkip(-1); // drop comma
            bytes.writeByte((byte) 'd');
            bytes.readPosition(0);
            assertEquals("abcd", bytes.readUtf8());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void excessiveNegativeSkipThrows() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(16);
        try {
            bytes.append("xx");
            // attempt to backtrack beyond start
            bytes.writeSkip(- (bytes.writePosition() + 2));
        } finally {
            bytes.releaseLast();
        }
    }
}
