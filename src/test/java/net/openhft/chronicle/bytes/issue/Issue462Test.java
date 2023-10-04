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
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class Issue462Test {

    static Stream<Bytes<ByteBuffer>> bytesToTest() {
        return Stream.of(
                Bytes.elasticByteBuffer(),
                Bytes.elasticByteBuffer(128),
                Bytes.elasticByteBuffer(128, 256),
                Bytes.elasticHeapByteBuffer(),
                Bytes.elasticHeapByteBuffer(128));
    }

    @ParameterizedTest
    @MethodSource("bytesToTest")
    public void testByteBufferByteOrder(Bytes<ByteBuffer> bytes) {
        final long value = 0x0102030405060708L;
        bytes.writeLong(value);
        final ByteBuffer byteBuffer = bytes.underlyingObject();
        assertEquals(ByteOrder.nativeOrder(), byteBuffer.order());
        final long aLong = byteBuffer.getLong();
        assertEquals(Long.toHexString(value), Long.toHexString(aLong));
        assertEquals(value, aLong);
        byteBuffer.putDouble(0, 0.1);
        final double actual = bytes.readDouble();
        assertEquals(Long.toHexString(Double.doubleToLongBits(0.1)), Long.toHexString(Double.doubleToLongBits(actual)));
        assertEquals(0.1, actual, 0.0);
    }
}