/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StopCharTester validates behaviour for standard tester variants")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class StopCharTesterTest {

    private static Stream<Arguments> stopCharCases() {
        return Stream.of(
                Arguments.of(StopCharTesters.COMMA_STOP, ',', true),
                Arguments.of(StopCharTesters.COMMA_STOP, 'A', false),
                Arguments.of(StopCharTesters.CURLY_STOP, '}', true),
                Arguments.of(StopCharTesters.CURLY_STOP, 'A', false),
                Arguments.of(StopCharTesters.COMMA_SPACE_STOP, ' ', true),
                Arguments.of(StopCharTesters.COMMA_SPACE_STOP, 'A', false),
                Arguments.of(StopCharTesters.CONTROL_STOP, '\n', true),
                Arguments.of(StopCharTesters.CONTROL_STOP, ' ', false),
                Arguments.of(StopCharTesters.SPACE_STOP, ' ', true),
                Arguments.of(StopCharTesters.SPACE_STOP, 'A', false),
                Arguments.of(StopCharTesters.QUOTES, '"', true),
                Arguments.of(StopCharTesters.QUOTES, 'A', false),
                Arguments.of(StopCharTesters.SINGLE_QUOTES, '\'', true),
                Arguments.of(StopCharTesters.SINGLE_QUOTES, 'A', false),
                Arguments.of(StopCharTesters.EQUALS, '=', true),
                Arguments.of(StopCharTesters.EQUALS, 'A', false),
                Arguments.of(StopCharTesters.NUMBER_END, '9', false),
                Arguments.of(StopCharTesters.NUMBER_END, 'x', true),
                Arguments.of(StopCharTesters.NON_ALPHA_DIGIT, 'A', false),
                Arguments.of(StopCharTesters.NON_ALPHA_DIGIT, '9', false),
                Arguments.of(StopCharTesters.NON_ALPHA_DIGIT, '!', true),
                Arguments.of(StopCharTesters.NON_NUL, 0, true),
                Arguments.of(StopCharTesters.NON_NUL, 'A', false),
                Arguments.of(StopCharTesters.ALL, -1, true),
                Arguments.of(StopCharTesters.ALL, 0, false)
        );
    }

    @Test
    @DisplayName("lambda stop tester returns expected values")
    public void testIsStopChar() {
        StopCharTester tester = ch -> ch == ';' || ch == ',';

        assertTrue(tester.isStopChar(';'),
                "Semicolon should be a stop character");
        assertTrue(tester.isStopChar(','),
                "Comma should be a stop character");
        assertFalse(tester.isStopChar('A'),
                "Letter should not be a stop character");
    }

    @Test
    @DisplayName("escaping stops on escapes before the underlying tester")
    public void testEscaping() {
        StopCharTester baseTester = ch -> ch == ';';
        StopCharTester escapingTester = baseTester.escaping();

        assertTrue(baseTester.isStopChar(';'),
                "Semicolon should be a stop character without escaping");
        assertFalse(escapingTester.isStopChar('\\'),
                "Escaping tester should treat backslash as escape");
    }

    @ParameterizedTest(name = "{index}: {0} isStopChar({1}) -> {2}")
    @MethodSource("stopCharCases")
    @DisplayName("standard StopCharTesters follow documented stop rules and exceptions")
    public void stopCharTestersMatchExpectedRules(StopCharTester tester, int ch, boolean expected) {
        if (expected) {
            assertTrue(tester.isStopChar(ch),
                    "StopCharTester should flag stop char for " + ch);
        } else {
            assertFalse(tester.isStopChar(ch),
                    "StopCharTester should allow non-stop char for " + ch);
        }
    }
}
