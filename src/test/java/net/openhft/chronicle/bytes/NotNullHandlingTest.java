/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.assertions.AssertUtil.SKIP_ASSERTIONS;
import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests defensive null and bounds handling for helper methods that accept
 * strings and lengths, ensuring consistent exception types are thrown.
 */
@DisplayName("Not null handling for defensive argument checks")
class NotNullHandlingTest extends BytesTestCommon {

    @Test
    @DisplayName("illegal arguments thrown for null or negative input")
    void fooMostCorrect() {
        assertThrows(IllegalArgumentException.class,
                () -> fooMostCorrect(null, 1),
                "Null input should trigger IllegalArgumentException in fooMostCorrect");
        assertThrows(IllegalArgumentException.class,
                () -> fooMostCorrect("a", -1),
                "Negative value should trigger IllegalArgumentException in fooMostCorrect");
    }

    @Test
    @DisplayName("null reference argument triggers NullPointerException before bounds validation")
    void fooCorrectTest() {
        assertThrows(NullPointerException.class,
                () -> fooCorrect(null, 1),
                "Null input should raise NullPointerException in fooCorrect");
        assertThrows(IllegalArgumentException.class,
                () -> fooCorrect("a", -1),
                "Negative value should raise IllegalArgumentException in fooCorrect");
    }

    @Test
    @DisplayName("assertion checks reject null references and negative bounds")
    void fooAssertTest() {
        assertThrows(NullPointerException.class,
                () -> fooAssert(null, 1),
                "Null input should raise NullPointerException in fooAssert");
        assertThrows(IllegalArgumentException.class,
                () -> fooAssert("a", -1),
                "Negative value should raise IllegalArgumentException in fooAssert");
    }

    private void fooMostCorrect(@NotNull CharSequence text, @NonNegative int value) {
        // Consistently throwing IllegalArgumentException for any failing invariant check
        if (text == null) // <- We could obviously create a method for this
            throw new IllegalArgumentException("Text argument must not be null value");
        requireNonNegative(value);
    }

    // Tests should pass if run both under IDEA or Maven
    // Hence, we sometimes need to set @NotNull(exception = NullPointerException.class)

    private void fooCorrect(@NotNull(exception = NullPointerException.class) CharSequence text, @NonNegative int value) {
        // Throws NullPointerException for reference parameters being null
        requireNonNull(text);
        // Throws IllegalArgumentException for other failing parameters
        requireNonNegative(value);
    }

    private void fooAssert(@NotNull(exception = NullPointerException.class) CharSequence text, @NonNegative int value) {
        // Checks will only be applied using -ea
        // This is generally ok for internal methods but not for methods in the public API. Almost all methods are public
        assert requireNonNull(text) != null : "Null text must fail assertion guard check";
        assert requireNonNegative(value) > 0 : "Non negative value must pass assertion check";
    }

    private void fooZeroCost(@NotNull(exception = NullPointerException.class) CharSequence text, @NonNegative int value) {
        // Checks will only be applied using -ea AND building a custom Core with SKIP_ASSERTIONS = false
        // and can otherwise not be enabled
        assert SKIP_ASSERTIONS || requireNonNull(text) != null
                : "Null text should fail zero cost assertion check";
        assert SKIP_ASSERTIONS || requireNonNegative(value) > 0
                : "Non negative value must pass zero cost check";
    }
}
