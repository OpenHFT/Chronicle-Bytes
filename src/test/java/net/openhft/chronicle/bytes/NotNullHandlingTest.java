/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.assertions.AssertUtil.SKIP_ASSERTIONS;
import static net.openhft.chronicle.core.util.Ints.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotNullHandlingTest extends BytesTestCommon {

    @Test
    void fooMostCorrect() {
        assertThrows(IllegalArgumentException.class, () -> fooMostCorrect(null, 1));
        assertThrows(IllegalArgumentException.class, () -> fooMostCorrect("a", -1));
    }

    @Test
    void fooCorrectTest() {
        assertThrows(NullPointerException.class, () -> fooCorrect(null, 1));
        assertThrows(IllegalArgumentException.class, () -> fooCorrect("a", -1));
    }

    @Test
    void fooAssertTest() {
        assertThrows(NullPointerException.class, () -> fooAssert(null, 1));
        assertThrows(IllegalArgumentException.class, () -> fooAssert("a", -1));
    }

    private void fooMostCorrect(@NotNull CharSequence text, @NonNegative int value) {
        // Consistently throwing IllegalArgumentException for any failing invariant check
        if (text == null) // <- We could obviously create a method for this
            throw new IllegalArgumentException();
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
        assert requireNonNull(text) != null;
        assert requireNonNegative(value) > 0;
    }

    private void fooZeroCost(@NotNull(exception = NullPointerException.class) CharSequence text, @NonNegative int value) {
        // Checks will only be applied using -ea AND building a custom Core with SKIP_ASSERTIONS = false
        // and can otherwise not be enabled
        assert SKIP_ASSERTIONS || requireNonNull(text) != null;
        assert SKIP_ASSERTIONS || requireNonNegative(value) > 0;
    }

    // This is where we are today
    private void fooUnsafe(@NotNull CharSequence text, int value) {
        // Potentially modifying internal state and only then throw an exception.
    }

}
