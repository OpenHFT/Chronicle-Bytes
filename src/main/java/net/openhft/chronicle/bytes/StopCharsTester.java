/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
     * @param ch         the character to test. If this is 0, it should return true or throw an exception.
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
