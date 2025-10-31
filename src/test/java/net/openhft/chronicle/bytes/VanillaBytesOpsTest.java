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

import static org.junit.Assert.assertEquals;

public class VanillaBytesOpsTest extends BytesTestCommon {

    @Test
    public void writeReadPrimitivesAndZeroOut() {
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        try {
            b.writeInt(0x11223344);
            b.writeLong(0x0102030405060708L);

            b.readPosition(0);
            assertEquals(0x11223344, b.readInt());
            assertEquals(0x0102030405060708L, b.readLong());

            // zero out the int we wrote and check
            b.zeroOut(0, 4);
            assertEquals(0, b.peekUnsignedByte(0));
            assertEquals(0, b.peekUnsignedByte(1));
        } finally {
            b.releaseLast();
        }
    }
}

