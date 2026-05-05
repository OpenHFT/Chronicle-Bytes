/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Issue 225 trailing zero handling checks")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class Issue225Test extends BytesTestCommon {
    @Test
    @DisplayName("append double preserves trailing zeros length")
    public void testTrailingZeros() {
        for (int i = 1000; i < 10_000; i++) {
            double value = i / 1000.0;
            final String valueStr;
            if ((long) value == value)
                valueStr = "" + (long) value;
            else
                valueStr = "" + value;
            Bytes<?> bytes = Bytes.allocateElastic(32);
            bytes.append(value);
            assertEquals(value, bytes.parseDouble(), 0.0,
                    "Parsed double matches appended value for index " + i);
            if ((long) value == value)
                assertEquals(0, bytes.lastDecimalPlaces(),
                        "Integer value has zero decimal places for index " + i);
            else
                assertEquals(valueStr.length() - 2, bytes.lastDecimalPlaces(),
                        "Decimal places align with string length for index " + i);
            bytes.readPosition(0);
            byte[] rbytes = new byte[24];
            int length = bytes.read(rbytes);
            assertEquals(valueStr.length(), length,
                    "Read length matches formatted value length for index " + i);
            final String substring = new String(rbytes, 0, (int) bytes.writePosition(),
                    StandardCharsets.ISO_8859_1);
            assertEquals(valueStr, substring,
                    "Round trip string output matches expected for index " + i);
            bytes.releaseLast();
        }
    }
}
