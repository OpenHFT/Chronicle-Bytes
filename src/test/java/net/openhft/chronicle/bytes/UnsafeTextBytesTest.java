/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.UnsafeText;
import net.openhft.chronicle.core.Maths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unsafe low-level byte operations because correct base-10 and double
 * encoding is essential for high-performance numeric serialisation.
 */
@SuppressWarnings({"deprecation", "checkstyle:MMOverusedWord", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("UnsafeTextBytes - validates unsafe numeric encoding")
class UnsafeTextBytesTest extends BytesTestCommon {

    private static void testAppendBase10(final Bytes<?> bytes, final long l) {
        final long address = bytes.clear().addressForRead(0);
        final long end = UnsafeText.appendFixed(address, l);
        bytes.readLimit(end - address);
        String message = bytes.toString();
        assertEquals(l,
                bytes.parseLong(),
                "Parsed long should match appended value " + l + " for text " + message);
    }

    static String testAppendDouble(final Bytes<?> bytes, final double l) {
        final long address = bytes.clear().addressForRead(0);
        final long end = UnsafeText.appendDouble(address, l);
        bytes.readLimit(end - address);
        final String message = bytes.toString();
        assertEquals(l,
                bytes.parseDouble(),
                Math.ulp(l),
                "Parsed double should match appended value " + l + " for text " + message);
        return message;
    }

    private static void testAppendFixed(final Bytes<?> bytes,
                                        final double l,
                                        final int digits) {
        final long address = bytes.clear().addressForRead(0);
        final long end = UnsafeText.appendFixed(address, l, digits);
        bytes.readLimit(end - address);
        final String message = bytes.toString();
        final double expected = Maths.round4(l);
        final double actual = bytes.parseDouble();
        assertEquals(expected,
                actual,
                0.0,
                "Fixed append should round to " + expected + " for digits " + digits + " from text " + message);
    }

    @Test
    @DisplayName("append fixed base-10 values round trip through parseLong")
    public void appendBase10() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            for (long l = Long.MAX_VALUE; l > 0; l /= 2) {
                testAppendBase10(bytes, l);
                testAppendBase10(bytes, 1 - l);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append double values round trip through parseDouble")
    public void appendDouble() {

        final Bytes<?> bytes = Bytes.allocateDirect(32);
        // testAppendFixed(bytes, 864960913420.1180, 4);
        testAppendFixed(bytes, 98472368148.9340, 4);
        testAppendFixed(bytes, 21.0607, 4);

        final Random rand = new Random(1);
        try {
            testAppendFixed(bytes, 0.0003, 4);
            for (int i = 0; i < 300000; i++) {
                double d = Math.pow(1e15, rand.nextDouble()) / 1e4;
                testAppendDouble(bytes, d);
                testAppendFixed(bytes, d, 4);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append double handles boundary values and infinities")
    public void appendDouble2() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            for (double d : new double[]{
                    741138311171.555,
                    0.0, -0.0, 0.1, 0.012, 0.00123, 1.0, 1 / 0.0, -1 / 0.0})
                testAppendDouble(bytes, d);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append double handles NaN boundary values")
    public void appendDoubleNan() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            final long address = bytes.clear().addressForRead(0);
            final long end = UnsafeText.appendDouble(address, Double.NaN);
            bytes.readLimit(end - address);
            final String message = bytes.toString();
            assertTrue(Double.isNaN(bytes.parseDouble()),
                    "Parsed NaN should remain NaN for text " + message);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append double preserves negative zero formatting")
    public void extraZeros() {
        final Bytes<?> bytes = Bytes.allocateDirect(32);
        try {
            final double d = -0.00002;
            final String output = testAppendDouble(bytes, d);
            assertEquals("-0.00002",
                    output,
                    "Negative zero formatting should keep leading zeros");
        } finally {
            bytes.releaseLast();
        }
    }
}
