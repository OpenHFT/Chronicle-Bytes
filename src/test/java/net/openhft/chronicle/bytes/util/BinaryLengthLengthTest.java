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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class BinaryLengthLengthTest {

    private final BinaryLengthLength binaryLengthLength;
    private final int binaryWireCode;

    public BinaryLengthLengthTest(BinaryLengthLength binaryLengthLength, int binaryWireCode) {
        this.binaryLengthLength = binaryLengthLength;
        this.binaryWireCode = binaryWireCode;
    }

    @Parameterized.Parameters(name = "binaryLengthLength {0} binaryWireCode {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BinaryLengthLength.LENGTH_8BIT, BinaryWireCode.BYTES_LENGTH8},
                {BinaryLengthLength.LENGTH_16BIT, BinaryWireCode.BYTES_LENGTH16},
                {BinaryLengthLength.LENGTH_32BIT, BinaryWireCode.BYTES_LENGTH32}
        });
    }

    @Test
    public void checkCodeMatches() {
        assertEquals(binaryWireCode, binaryLengthLength.code());
    }

    @Test
    public void checkCodeIsWritten() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer(128);
        binaryLengthLength.initialise(bytes);
        byte readCode = (byte) bytes.readUnsignedByte();
        assertEquals((byte) binaryWireCode, readCode);
    }

}