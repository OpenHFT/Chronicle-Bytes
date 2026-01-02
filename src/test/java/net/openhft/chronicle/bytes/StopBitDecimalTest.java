/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class StopBitDecimalTest extends BytesTestCommon {
    @Test
    @DisplayName("stop-bit decimal round trip preserves scale and value")
    public void testDecimals() {
        assumeFalse(NativeBytes.areNewGuarded(), "Stop-bit decimal test requires unguarded native bytes");

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
            assertEquals(bd.doubleValue(),
                    ebd.doubleValue(),
                    0.0,
                    "Stop-bit raw value should match decimal at i=" + i + ", d=" + d + ", v=" + v);
            bytes.readPosition(0);
            double d2 = bytes.readStopBitDecimal();
            assertEquals(d,
                    d2,
                    0.0,
                    "Stop-bit decimal round trip should match at i=" + i + ", d=" + d + ", v=" + v);
        }
    }
}
