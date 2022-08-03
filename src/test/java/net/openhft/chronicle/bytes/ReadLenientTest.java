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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeFalse;

public class ReadLenientTest extends BytesTestCommon {
    @Test
    public void testLenient() {
        assumeFalse(NativeBytes.areNewGuarded());
        doTest(Bytes.allocateDirect(64));
        doTest(Bytes.allocateElasticOnHeap(64));
        doTest(Bytes.from(""));
    }

    @SuppressWarnings("rawtypes")
    private void doTest(Bytes<?> bytes)
            throws BufferUnderflowException, ArithmeticException, IllegalArgumentException {
        bytes.lenient(true);
        ByteBuffer bb = ByteBuffer.allocateDirect(32);
        bytes.read(bb);
        assertEquals(0, bb.position());

        assertEquals(BigDecimal.ZERO, bytes.readBigDecimal());
        assertEquals(BigInteger.ZERO, bytes.readBigInteger());
        assertFalse(bytes.readBoolean());
        assertEquals("", bytes.read8bit());
        assertEquals("", bytes.readUtf8());
        assertEquals(0, bytes.readByte());
        assertEquals(-1, bytes.readUnsignedByte()); // note this behaviour is need to find the end of a stream.
        assertEquals(0, bytes.readShort());
        assertEquals(0, bytes.readUnsignedShort());
        assertEquals(0, bytes.readInt());
        assertEquals(0, bytes.readUnsignedInt());
        assertEquals(0.0, bytes.readFloat(), 0.0);
        assertEquals(0.0, bytes.readDouble(), 0.0);
        bytes.readSkip(8);
        assertEquals(0, bytes.readPosition());

        bytes.releaseLast();
    }
}
