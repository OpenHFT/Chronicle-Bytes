/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.StopCharTester;

/**
 * Implementation of {@link StopCharTester} where a backslash ('\\') escapes the
 * following character. The tester delegates to another {@code StopCharTester}
 * for actual stop character detection of non escaped characters.
 * <p>
 * The escape state persists for a single subsequent call to
 * {@link #isStopChar(int)} on the same instance. Instances are therefore not
 * thread safe. See also {@link EscapingStopCharsTester} for the two-argument
 * variant.
 */
public class EscapingStopCharTester implements StopCharTester {

    // The decorated StopCharTester
    private final StopCharTester sct;
    // A flag to track whether the last character was an escape character
    private boolean escaped = false;

    /**
     * @param sct the underlying {@link StopCharTester} used when the character
     *            is not escaped
     */
    public EscapingStopCharTester(StopCharTester sct) {
        this.sct = sct;
    }

    /**
     * Tests whether the given character should be considered as a stop character.
     * A character immediately after an escape character ('\\') is never considered a stop character.
     *
     * @param ch the character to test
     * @return {@code true} if the character is a stop character, {@code false} otherwise
     */
    @Override
    public boolean isStopChar(int ch) {
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

        // Delegate to the decorated StopCharTester
        return sct.isStopChar(ch);
    }
}
