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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.EscapingStopCharsTester;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface that defines a strategy for identifying stop characters during a string parsing operation.
 * This allows customization of how and where string parsing should terminate.
 */
@FunctionalInterface
public interface StopCharsTester {
    /**
     * Determines whether the provided character should cause the string parsing operation to stop.
     * The logic for deciding this can be custom defined for different scenarios.
     *
     * <p>
     * For safety reasons, it is advised that a byte of value 0 should either be defined as a stop character
     * or should throw an IllegalStateException to prevent issues with null-terminated strings.
     *
     * @param ch  the character to test. If this is 0, it should return true or throw an exception.
     * @param peekNextCh the next character that would be parsed after 'ch'. It can be used for context aware stopping, like identifying escape sequences.
     * @return true if the provided character is a stop character, false otherwise.
     */
    boolean isStopChar(int ch, int peekNextCh);

    /**
     * Creates a new  that respects escape characters in the string parsing operation.
     * This allows for more complex string parsing scenarios where certain stop characters may need to be ignored
     * if they are escaped.
     *
     * @return A new  that respects escape characters.
     */
    @NotNull
    default StopCharsTester escaping() {
        return new EscapingStopCharsTester(this);
    }
}
