/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VanillaBytesTest extends BytesTestCommon {

    @Test
    void testBytesForRead() {
        byte[] byteArr = new byte[128];
        for (int i = 0; i < byteArr.length; i++)
            byteArr[i] = (byte) i;
        Bytes<?> bytes = Bytes.wrapForRead(byteArr);
        bytes.readSkip(8);
        @NotNull Bytes<?> bytes2 = bytes.bytesForRead();
        assertEquals(128 - 8, bytes2.readRemaining());
        assertEquals(8, bytes2.readPosition());
        assertEquals(8, bytes2.readByte(bytes2.start()));
        assertEquals(9, bytes2.readByte(bytes2.start() + 1));
        assertEquals(9, bytes.readByte(9));
        bytes2.writeByte(bytes2.start() + 1, 99);
        assertEquals(99, bytes.readByte(99));

        bytes.releaseLast();
    }
}