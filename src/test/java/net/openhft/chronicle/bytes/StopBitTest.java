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

import org.junit.Assert;
import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StopBitTest extends BytesTestCommon {

    @Test
    public void testStopBit() {

        for (int i = 0; i < (1 << 10) + 1; i++) {
            final String expected = IntStream.range(0, i)
                    .mapToObj(Integer::toString)
                    .collect(Collectors.joining());

            final Bytes<byte[]> expectedBytes = Bytes.from(expected);

            final Bytes<ByteBuffer> b = Bytes.elasticByteBuffer();
            try {
                if (expectedBytes == null) {
                    b.writeStopBit(-1);
                } else {
                    long offset = expectedBytes.readPosition();
                    long readRemaining = Math.min(b.writeRemaining(), expectedBytes.readLimit() - offset);
                    b.writeStopBit(readRemaining);
                    try {
                        b.write(expectedBytes, offset, readRemaining);
                    } catch (BufferUnderflowException | IllegalArgumentException e) {
                        throw new AssertionError(e);
                    }
                }

                // System.out.printf("0x%04x : %02x %02x %02x%n", i, b.readByte(0), b.readByte(1), b.readByte(3));

                Assert.assertEquals("failed at " + i, expected, b.read8bit());

            } finally {
                b.releaseLast();
                expectedBytes.releaseLast();
            }
        }
    }

    @Test
    public void testStopBitShort() {

        final String s = IntStream.range(0, 1)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());

        final Bytes<byte[]> bytes = Bytes.from(s);

        final Bytes<ByteBuffer> b = Bytes.elasticByteBuffer();
        try {
            if (bytes == null) {
                b.writeStopBit(-1);
            } else {
                long offset = bytes.readPosition();
                long readRemaining = Math.min(b.writeRemaining(), bytes.readLimit() - offset);
                b.writeStopBit(readRemaining);
                try {
                    b.write(bytes, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError(e);
                }
            }

            Assert.assertEquals(s, b.read8bit());
        } finally {
            bytes.releaseLast();
            b.releaseLast();
        }
    }

}
