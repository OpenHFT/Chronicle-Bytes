/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.util.EscapingStopCharTester;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface for defining a strategy to identify stop characters during string parsing.
 * It allows customization of the end condition for the parsing operation.
 */
@FunctionalInterface
public interface StopCharTester {

    /**
     * Tests if the provided character should be regarded as a stop character during a string parsing operation.
     * The specific logic for this determination can be defined per use case.
     *
     * <p>
     * Note: For safety reasons, a character of value 0 should either return true (i.e., be considered a stop character)
     * or throw an IllegalStateException. This is to prevent issues with null-terminated strings.
     *
     * @param ch the character to be tested. If ch is 0, the method should return true or throw an exception.
     * @return true if the provided character is a stop character, false otherwise.
     */
    boolean isStopChar(int ch);

    /**
     * Returns a new  instance that respects escape characters.
     * This allows for parsing scenarios where certain stop characters should be ignored if they are escaped.
     *
     * @return a new  that takes escape characters into account.
     */
    @NotNull
    default StopCharTester escaping() {
        return new EscapingStopCharTester(this);
    }
}
