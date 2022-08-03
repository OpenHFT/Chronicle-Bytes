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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

public class Issue281Test extends BytesTestCommon {
    public static void bufferToBytes(Bytes<?> bytes, ByteBuffer dataBuffer, int index) {
        int length = dataBuffer.get(index); // length prefix (offset)
        bytes.write(0, dataBuffer, index + 1, length);
        bytes.writeSkip(length);
    }

    @Test
    public void testByteBufferToBytes() {
        final Bytes<?> data = Bytes.allocateElasticDirect().append("1234567890ABCD");
        final Bytes<?> retVal = Bytes.allocateElasticDirect();
        ByteBuffer buffer = ByteBuffer.allocateDirect(16);
        String test = "1234567890ABCD";
        buffer.put((byte) test.length());
        for (byte b : test.getBytes(StandardCharsets.UTF_8)) {
            buffer.put(b);
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        bufferToBytes(retVal, buffer, 0); // this calls bufferToBytes below
        assertTrue(data.contentEquals(retVal));

        retVal.clear();
        buffer.order(ByteOrder.BIG_ENDIAN);
        bufferToBytes(retVal, buffer, 0); // this calls bufferToBytes below
        assertTrue(data.contentEquals(retVal));
    }
}
