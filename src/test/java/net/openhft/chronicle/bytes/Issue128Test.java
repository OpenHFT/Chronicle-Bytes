/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

import static net.openhft.chronicle.bytes.UnsafeTextBytesTest.testAppendDouble;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Issue 128 decimal formatting validation checks")
public class Issue128Test extends BytesTestCommon {
    private static final DecimalFormat DF;

    static {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(30);
        DF = df;
    }

    @Test
    @DisplayName("append double matches decimal formatting in odd cases")
    public void testCorrect() {
        Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            // odd ones are trouble.
            for (int i = 1; i < 1_000_000; i += 2) {
                double v6 = (double) i / 1_000_000;
                doTest(bytes, v6);
                doTest(bytes, 999 + v6);
                double v7 = (double) i / 10_000_000;
                doTest(bytes, v7);
                double v8 = (double) i / 100_000_000;
                doTest(bytes, v8);
                double v9 = (double) i / 1_000_000_000;
                doTest(bytes, v9);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    private void doTest(Bytes<?> bytes, double v) {
        String format = DF.format(v);
        String output = testAppendDouble(bytes, v);
        if (Double.parseDouble(output) != v || format.length() != output.length()) {
            // Don't compare strings if we've added an exponent
            if (!output.contains("E")) {
                assertEquals(DF.format(v), output,
                        "Decimal formatted output matches for value " + v);
            } else {
                assertEquals(v, Double.parseDouble(output), 0.0,
                        "Parsed output matches numeric value " + v);
            }
        }
    }
}
