/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharsTester;

/**
 * Implementation of {@link StopCharsTester} that treats a backslash ('\\') as an
 * escape character. The tester delegates to another {@code StopCharsTester} when
 * a character is not escaped.
 * <p>
 * The escape state is held per instance: after seeing a backslash the next call
 * to {@link #isStopChar(int, int)} ignores the character passed and clears the
 * escaped state. Instances are therefore not thread safe. See also
 * {@link EscapingStopCharTester} for the single-character variant.
 */
public class EscapingStopCharsTester implements StopCharsTester {

    private final StopCharsTester sct;
    // A flag to track whether the last character was an escape character
    private boolean escaped = false;

    /**
     * @param sct the underlying {@link StopCharsTester} to which non escaped
     *            characters will be delegated
     */
    public EscapingStopCharsTester(StopCharsTester sct) {
        this.sct = sct;
    }

    /**
     * Tests whether the given character should be considered as a stop character.
     * A character immediately after an escape character ('\\') is never considered a stop character.
     *
     * @param ch         the character to test
     * @param peekNextCh the next character (peeked ahead)
     * @return {@code true} if the character is a stop character, {@code false} otherwise
     */
    @Override
    public boolean isStopChar(int ch, int peekNextCh) {
        // If the previous character was an escape character, do not treat 'ch' as a stop character
        if (escaped) {
            escaped = false;
            return false;
        }

        // If the current character is an escape character, set the flag and do not treat it as a stop character
        if (ch == '\\') {
            escaped = true;
            return false;
        }

        // Delegate to the decorated StopCharsTester
        return sct.isStopChar(ch, peekNextCh);
    }
}
