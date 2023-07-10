/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.bytes.StopCharsTester;

/**
 * A custom implementation of {@link StopCharsTester} that considers escape characters.
 * This tester allows escaping of characters using the backslash "\\" and will not treat
 * an escaped character as a stop character.
 */
public class EscapingStopCharsTester implements StopCharsTester {

    // The decorated StopCharsTester
    private final StopCharsTester sct;
    // A flag to track whether the last character was an escape character
    private boolean escaped = false;

    /**
     * Constructs an EscapingStopCharsTester with the given {@link StopCharsTester}.
     *
     * @param sct the StopCharsTester to be decorated with escape character functionality.
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
