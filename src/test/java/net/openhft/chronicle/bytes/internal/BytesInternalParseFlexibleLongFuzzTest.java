/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BytesInternalParseFlexibleLongFuzzTest extends BytesTestCommon {
    private final String value;
    private final long expected;

    public BytesInternalParseFlexibleLongFuzzTest(String value, long expected) {
        this.value = value;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"", 0L},
                {" ", 0L},
                {"-", 0L},
                {"+", 0L},
                {".", 0L},
                {"-.", 0L},
                {"+.", 0L},
                {"1e", 1L},
                {"1E", 1L},
                {"1e+", 1L},
                {"1e-", 1L},
                {"1.e", 1L},
                {"1.E+", 1L},
                {"0e", 0L},
                {"0e+", 0L},
                {"0e-", 0L},
                {"1..0", 1L},
                {"1.0.0", 1L},
                {"10.0.0e1", 100L},
                {"10000.0.0e-3", 10L},
                {"6e6", 6_000_000L},
                {"6000000", 6_000_000L},
                {"10000e-3", 10L},
                {"9.007199254740993e15", 9_007_199_254_740_993L},
                {"1.000000000000000001e18", 1_000_000_000_000_000_001L}
        });
    }

    @Test
    public void parsesLenientFlexibleLongCase() {
        Bytes<?> bytes = Bytes.from(value + ' ');
        try {
            assertEquals("Unexpected value for " + printable(value), expected, BytesInternal.parseFlexibleLong(bytes));
        } finally {
            bytes.releaseLast();
        }
    }

    private static String printable(String value) {
        return value.isEmpty() ? "<empty>" : value;
    }
}
