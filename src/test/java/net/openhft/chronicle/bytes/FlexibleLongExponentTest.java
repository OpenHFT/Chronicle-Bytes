/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlexibleLongExponentTest extends BytesTestCommon {
    private void rejects(String text) {
        Bytes<?> bytes = Bytes.from(text);
        try {
            assertThrows(text, IORuntimeException.class, bytes::parseFlexibleLong);
        } finally {
            bytes.releaseLast();
        }
    }

    private void parses(String text, long expected) {
        Bytes<?> bytes = Bytes.from(text);
        try {
            assertEquals(text, expected, bytes.parseFlexibleLong());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void exponentsCannotWrapAtIntegerWidth() {
        rejects("1e4294967296");
        rejects("1e-4294967296");
        rejects("1e-2147483648");
    }

    @Test
    public void exponentAccumulationCannotWrapAtLongWidth() {
        rejects("1e18446744073709551616");
        rejects("1e-18446744073709551616");
    }

    @Test
    public void minimumScaleCannotOverflowWhenNegated() {
        rejects("1.0e-9223372036854775807");
        rejects("1.00e-9223372036854775807");
    }

    @Test(timeout = 2000)
    public void zeroWithLargeExponentDoesNotLoopOverTheExponent() {
        parses("0e2147483647", 0);
        parses("0e-2147483647", 0);
        parses("0e4294967296", 0);
        parses("0e18446744073709551616", 0);
        parses("0e-18446744073709551616", 0);
    }

    @Test
    public void zeroRetainsParserMetadata() {
        Bytes<?> bytes = Bytes.from("0e20");
        try {
            assertEquals(0, bytes.parseFlexibleLong());
            assertTrue(bytes.lastNumberHadDigits());
            assertEquals(0, bytes.lastDecimalPlaces());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void scaledLongBoundariesRemainExact() {
        parses("92233720368547758070e-1", Long.MAX_VALUE);
        parses("-92233720368547758080e-1", Long.MIN_VALUE);
        parses("12300e-2", 123);
        parses("12e+2", 1200);
        rejects("12e-1");
    }

    @Test
    public void closingDelimiterRemainsAvailableToTheCaller() {
        Bytes<?> bytes = Bytes.from("12e2]");
        try {
            assertEquals(1200, bytes.parseFlexibleLong());
            assertEquals(']', bytes.readUnsignedByte());
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void followingNumberRemainsReadable() {
        Bytes<?> bytes = Bytes.from("12e2,34");
        try {
            assertEquals(1200, bytes.parseFlexibleLong());
            assertEquals(34, bytes.parseLong());
        } finally {
            bytes.releaseLast();
        }
    }
}
