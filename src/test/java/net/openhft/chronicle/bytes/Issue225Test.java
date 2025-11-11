/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Issue225Test extends BytesTestCommon {
    @Test
    public void testTrailingZeros() {
        for (int i = 1000; i < 10_000; i++) {
            double value = i / 1000.0;
            final String valueStr;
            if ((long) value == value)
                valueStr = "" + (long) value;
            else
                valueStr = "" + value;
            Bytes<?> bytes = Bytes.allocateElastic(32);
            byte[] rbytes = new byte[24];
            bytes.append(value);
            assertEquals(value, bytes.parseDouble(), 0.0);
            if ((long) value == value)
                assertEquals(0, bytes.lastDecimalPlaces());
            else
                assertEquals(valueStr.length() - 2, bytes.lastDecimalPlaces());
            bytes.readPosition(0);
            int length = bytes.read(rbytes);
            assertEquals(valueStr.length(), length);
            final String substring = new String(rbytes).substring(0, (int) bytes.writePosition());
            assertEquals(valueStr, substring);
            bytes.releaseLast();
        }
    }
}
