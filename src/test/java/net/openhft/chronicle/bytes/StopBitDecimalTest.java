/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.core.Maths;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assume.assumeFalse;

public class StopBitDecimalTest extends BytesTestCommon {
    @Test
    public void testDecimals() {
        assumeFalse(NativeBytes.areNewGuarded());

        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(16);
        Random rand = new Random();
        for (int i = 0; i < 10_000; i++) {
            rand.setSeed(i);
            bytes.clear();
            int scale = rand.nextInt(10);
            double d = (rand.nextLong() % 1e14) / Maths.tens(scale);
            bytes.writeStopBitDecimal(d);
            BigDecimal bd = BigDecimal.valueOf(d);
            long v = bytes.readStopBit();
            BigDecimal ebd = new BigDecimal(BigInteger.valueOf(v / 10), (int) (Math.abs(v) % 10));
            assertEquals("i: " + i + ", d: " + d + ", v: " + v, ebd.doubleValue(), bd.doubleValue(), 0.0);
            bytes.readPosition(0);
            double d2 = bytes.readStopBitDecimal();
            assertEquals("i: " + i + ", d: " + d + ", v: " + v, d, d2, 0.0);
        }
    }
}
