/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BytesInternal parsing for booleans and UTF8 segments")
public class BytesInternalParsingTest extends BytesTestCommon {

    @Test
    @DisplayName("parse boolean tokens with stop char testers")
    public void parseBooleanTokens() {
        Bytes<?> t = Bytes.from("true");
        Bytes<?> f = Bytes.from("false");
        Bytes<?> y = Bytes.from("yes");
        Bytes<?> n = Bytes.from("no");
        Bytes<?> one = Bytes.from("1");
        Bytes<?> zero = Bytes.from("0");
        Bytes<?> maybe = Bytes.from("maybe");
        try {
            assertEquals(Boolean.TRUE,
                    BytesInternal.parseBoolean(t, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean true token");
            assertEquals(Boolean.FALSE,
                    BytesInternal.parseBoolean(f, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean false token");
            assertEquals(Boolean.TRUE,
                    BytesInternal.parseBoolean(y, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean yes token");
            assertEquals(Boolean.FALSE,
                    BytesInternal.parseBoolean(n, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean no token");
            assertEquals(Boolean.TRUE,
                    BytesInternal.parseBoolean(one, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean one token");
            assertEquals(Boolean.FALSE,
                    BytesInternal.parseBoolean(zero, StopCharTesters.NON_ALPHA_DIGIT),
                    "Parse boolean zero token");
            assertNull(BytesInternal.parseBoolean(maybe, StopCharTesters.NON_ALPHA_DIGIT),
                    "Unknown boolean token yields null");
        } finally {
            t.releaseLast();
            f.releaseLast();
            y.releaseLast();
            n.releaseLast();
            one.releaseLast();
            zero.releaseLast();
            maybe.releaseLast();
        }
    }

    @Test
    @DisplayName("parse UTF8 into builder with stop tester")
    public void parseUtf8IntoBuilder() {
        Bytes<?> alpha = Bytes.from("alpha");
        Bytes<?> beta = Bytes.from("beta");
        try {
            StringBuilder sb = new StringBuilder();
            BytesInternal.parseUtf8(alpha, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("alpha", sb.toString(),
                    "UTF8 parse reads alpha");
            sb.setLength(0);
            BytesInternal.parseUtf8(beta, sb, StopCharTesters.NON_ALPHA_DIGIT);
            assertEquals("beta", sb.toString(),
                    "UTF8 parse reads beta");
        } finally {
            alpha.releaseLast();
            beta.releaseLast();
        }
    }

    @ParameterizedTest
    @DisplayName("parseDouble handles NaN and infinity inputs")
    @CsvSource({
            "NaN, NaN",
            "Infinity, Infinity",
            "-Infinity, -Infinity",
            "+Infinity, Infinity",
            "  NaN, NaN",
            "  Infinity, Infinity"
    })
    void parseDoubleSpecialValues(String input, String expected) {
        Bytes<?> bytes = Bytes.from(input);
        try {
            double result = BytesInternal.parseDouble(bytes);
            if ("NaN".equals(expected)) {
                assertTrue(Double.isNaN(result),
                        "parseDouble should return NaN for input '" + input + "'");
            } else if ("Infinity".equals(expected)) {
                assertEquals(Double.POSITIVE_INFINITY, result,
                        "parseDouble should return POSITIVE_INFINITY for input '" + input + "'");
            } else if ("-Infinity".equals(expected)) {
                assertEquals(Double.NEGATIVE_INFINITY, result,
                        "parseDouble should return NEGATIVE_INFINITY for input '" + input + "'");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest
    @DisplayName("parseDouble parses numeric and exponent inputs")
    @CsvSource({
            "0, 0.0",
            "1, 1.0",
            "-1, -1.0",
            "123.456, 123.456",
            "-123.456, -123.456",
            "+123.456, 123.456",
            "1e10, 1.0E10",
            "1E10, 1.0E10",
            "1.5e2, 150.0",
            "-1.5e2, -150.0",
            "1e-3, 0.001"
    })
    void parseDoubleNumericValues(String input, double expected) {
        Bytes<?> bytes = Bytes.from(input);
        try {
            double result = BytesInternal.parseDouble(bytes);
            assertEquals(expected, result, 1e-10,
                    "parseDouble should return " + expected + " for input '" + input + "'");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parse double returns -0.0 for non-digit input")
    void parseDoubleNonDigit() {
        Bytes<?> bytes = Bytes.from("abc");
        try {
            double result = BytesInternal.parseDouble(bytes);
            assertEquals(-0.0, result, "Non-digit input should return -0.0");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest
    @DisplayName("parseLong parses decimal and signed inputs")
    @CsvSource({
            "0, 0",
            "1, 1",
            "-1, -1",
            "123, 123",
            "-123, -123",
            "+456, 456",
            "  789, 789",
            "9223372036854775807, 9223372036854775807",
            "-9223372036854775808, -9223372036854775808"
    })
    void parseLongValues(String input, long expected) {
        Bytes<?> bytes = Bytes.from(input);
        try {
            long result = BytesInternal.parseLong(bytes);
            assertEquals(expected, result,
                    "parseLong should return " + expected + " for input '" + input + "'");
        } finally {
            bytes.releaseLast();
        }
    }

    @ParameterizedTest
    @DisplayName("parseHexLong parses hexadecimal input values correctly")
    @CsvSource({
            "0, 0",
            "a, 10",
            "A, 10",
            "ff, 255",
            "FF, 255",
            "100, 256",
            "deadbeef, 3735928559"
    })
    void parseHexLongValues(String input, long expected) {
        Bytes<?> bytes = Bytes.from(input);
        try {
            long result = BytesInternal.parseHexLong(bytes);
            assertEquals(expected, result,
                    "parseHexLong should return " + expected + " for input '" + input + "'");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("parse long decimal with fractional parts")
    void parseLongDecimalValues() {
        Bytes<?> bytes1 = Bytes.from("123.456");
        Bytes<?> bytes2 = Bytes.from("100.0");
        Bytes<?> bytes3 = Bytes.from("-50.5");
        try {
            long result1 = BytesInternal.parseLongDecimal(bytes1);
            assertEquals(123456, result1, "Parse long decimal with fraction");

            long result2 = BytesInternal.parseLongDecimal(bytes2);
            assertEquals(1000, result2, "Parse long decimal with trailing zeros");

            long result3 = BytesInternal.parseLongDecimal(bytes3);
            assertEquals(-505, result3, "Parse negative long decimal");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
            bytes3.releaseLast();
        }
    }

    @Test
    @DisplayName("parseFlexibleLong handles signed inputs with exponent suffix")
    void parseFlexibleLongValues() {
        Bytes<?> bytes1 = Bytes.from("1e3");
        Bytes<?> bytes2 = Bytes.from("5e2");
        Bytes<?> bytes3 = Bytes.from("-2e4");
        try {
            long result1 = BytesInternal.parseFlexibleLong(bytes1);
            assertEquals(1000, result1, "Parse flexible long with exponent");

            long result2 = BytesInternal.parseFlexibleLong(bytes2);
            assertEquals(500, result2, "Parse flexible long 5e2");

            long result3 = BytesInternal.parseFlexibleLong(bytes3);
            assertEquals(-20000, result3, "Parse negative flexible long");
        } finally {
            bytes1.releaseLast();
            bytes2.releaseLast();
            bytes3.releaseLast();
        }
    }
}
