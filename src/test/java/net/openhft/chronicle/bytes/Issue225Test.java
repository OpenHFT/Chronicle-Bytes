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

import static org.junit.Assert.assertEquals;

public class Issue225Test extends BytesTestCommon {
    @Test
    public void testTrailingZeros() {
        for (int i = 1000; i < 10_000; i++) {
            double value = i / 1000.0;
            final String valueStr = "" + value;
            Bytes<?> bytes = Bytes.elasticByteBuffer();
            byte[] rbytes = new byte[24];
            bytes.append(value);
            assertEquals(value, bytes.parseDouble(), 0.0);
            assertEquals(valueStr.length() - 2, bytes.lastDecimalPlaces());
            bytes.readPosition(0);
            int length = bytes.read(rbytes);
            assertEquals(valueStr.length(), length);
            final String substring = new String(rbytes).substring(0, (int) bytes.writePosition());
            assertEquals(valueStr, substring);
        }
    }
}
