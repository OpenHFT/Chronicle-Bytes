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

import net.openhft.chronicle.bytes.internal.ByteStringReader;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * An interface that provides support for parsing bytes as text. This interface is designed to facilitate
 * the reading and parsing of textual data that is encoded within a byte stream. It supports parsing
 * various data types including booleans, integers, floats, doubles, and decimal numbers,
 * and provides utility methods for dealing with character encoding such as UTF-8 and ISO-8859-1.
 *
 * <p>The {@code ByteStringParser} extends {@link StreamingDataInput}, inheriting the capabilities
 * of reading and manipulating streams of binary data.</p>
 *
 * <p>This interface is especially useful for reading and converting bytes into human-readable text
 * or numerical representations. It includes methods for parsing numbers with flexible formatting,
 * reading text with specified character encoding, and utilities for dealing with character
 * termination conditions.</p>
 *
 * <p>Example use cases include reading and parsing data from binary communication protocols,
 * files, or any other source where bytes need to be interpreted as text or numbers.</p>
 *
 * @param <B> the type of {@code ByteStringParser} which extends itself, allowing for method chaining.
 */
public interface ByteStringParser<B extends ByteStringParser<B>> extends StreamingDataInput<B> {
    /**
     * Creates a new {@code Reader} from the byte string, assuming the byte string is
     * ISO-8859-1 encoded.
     *
     * @return a new {@code Reader} instance that can be used to read the bytes as characters.
     */
    @NotNull
    default Reader reader() {
        return new ByteStringReader(this);
    }

    /**
     * Attempts to parse the next series of characters in the byte string as a boolean value.
     * It uses the provided {@code tester} to detect the end of the text. The parsing is case-insensitive.
     * <p>
     * False can be: "f", "false", "n", "no", "0".
     * True can be: "t", "true", "y", "yes", "1".
     *
     * @param tester a {@code StopCharTester} used to detect the end of the boolean text.
     * @return a {@code Boolean} value if the text could be parsed as boolean; null otherwise.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     * @throws ArithmeticException      if a numeric overflow occurs.
     */
    @Nullable
    default Boolean parseBoolean(@NotNull StopCharTester tester)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.parseBoolean(this, tester);
    }

    /**
     * Attempts to parse the next series of characters in the byte string as a boolean value.
     * It uses a default {@code StopCharTester} to detect non-alpha-numeric characters.
     * <p>
     * False can be: "f", "false", "n", "no", "0".
     * True can be: "t", "true", "y", "yes", "1".
     *
     * @return a {@code Boolean} value if the text could be parsed as boolean; null otherwise.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     * @throws ArithmeticException      if a numeric overflow occurs.
     */
    @Nullable
    default Boolean parseBoolean()
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.parseBoolean(this, StopCharTesters.NON_ALPHA_DIGIT);
    }

    /**
     * Parses a UTF-8 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition.
     *
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @return the parsed text as a {@code String}.
     * @throws IllegalStateException if this operation cannot be performed currently.
     * @throws ArithmeticException   if a numeric overflow occurs.
     */
    @NotNull
    default String parseUtf8(@NotNull StopCharTester stopCharTester)
            throws IllegalStateException, ArithmeticException {
        return BytesInternal.parseUtf8(this, stopCharTester);
    }

    /**
     * Parses a UTF-8 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer         the {@code Appendable} to append the parsed string to.
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     * @throws ArithmeticException      if a numeric overflow occurs.
     */
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        BytesInternal.parseUtf8(this, buffer, stopCharTester);
    }

    /**
     * Parses a UTF-8 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code Appendable} to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IORuntimeException       if an I/O error occurs.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        BytesInternal.parseUtf8(this, buffer, stopCharsTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer         the {@code Appendable} to append the parsed string to.
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws BufferOverflowException  if the buffer's capacity was exceeded.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     * @throws ArithmeticException      if a numeric overflow occurs.
     */
    default void parse8bit(Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException, ArithmeticException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition.
     *
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @return the parsed text as a {@code String}.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default String parse8bit(@NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parse8bit(this, stopCharTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code Appendable} to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws BufferOverflowException  if the buffer's capacity was exceeded.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     * @throws ArithmeticException      if a numeric overflow occurs.
     */
    default void parse8bit(Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException, ArithmeticException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharsTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharsTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code Bytes} object to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws BufferOverflowException  if the buffer's capacity was exceeded.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     * @throws ArithmeticException      if a numeric overflow occurs.
     */
    default void parse8bit(Bytes<?> buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException, ArithmeticException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code StringBuilder} to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws IllegalStateException if this operation cannot be performed currently.
     */
    default void parse8bit(StringBuilder buffer, @NotNull StopCharsTester stopCharsTester)
            throws IllegalStateException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    /**
     * Parses text from the byte string as an integer. The terminating character is consumed.
     *
     * @return the parsed integer.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws ArithmeticException      if a numeric overflow occurs.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default int parseInt()
            throws BufferUnderflowException, ArithmeticException, IllegalStateException {
        return Maths.toInt32(BytesInternal.parseLong(this));
    }

    /**
     * Parses text from the byte string as a long integer. The terminating character is consumed.
     *
     * @return the parsed long.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default long parseLong()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseLong(this);
    }

    /**
     * Parses a long integer from the byte string in either standard or scientific notation. If the parsed value
     * corresponds to a valid long, it returns the value without any precision errors (unlike casting the result
     * of {@link #parseDouble()} to long).
     *
     * @return the parsed long.
     * @throws IORuntimeException       if the parsed value corresponds to a fractional number or to a number outside the long's range.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default long parseFlexibleLong()
            throws BufferUnderflowException, IllegalStateException, IORuntimeException {
        return BytesInternal.parseFlexibleLong(this);
    }

    /**
     * Parses text from the byte string as a floating-point number. The terminating character is consumed.
     * The number of decimal places can be retrieved with {@code lastDecimalPlaces()}.
     *
     * @return the parsed float, or -0.0 if there were no digits.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default float parseFloat()
            throws BufferUnderflowException, IllegalStateException {
        return (float) BytesInternal.parseDouble(this);
    }

    /**
     * Parses text from the byte string as a double-precision floating-point number. The terminating character is consumed.
     * The number of decimal places can be retrieved with {@code lastDecimalPlaces()}.
     *
     * @return the parsed double, or -0.0 if there were no digits.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default double parseDouble()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseDouble(this);
    }

    /**
     * Parses the significant digits of a decimal number from the byte string.
     * The number of decimal places can be retrieved with {@code lastDecimalPlaces()}.
     *
     * @return the significant digits as a long.
     * @throws BufferUnderflowException if there is insufficient data.
     * @throws IllegalStateException    if this operation cannot be performed currently.
     */
    default long parseLongDecimal()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseLongDecimal(this);
    }

    /**
     * Returns the number of decimal places in the last parsed floating-point number
     * (from the {@link #parseDouble()} or {@link #parseLongDecimal()} methods).
     *
     * @return the number of decimal places in the last parsed number.
     */
    int lastDecimalPlaces();

    /**
     * Sets the number of decimal places in the last parsed number. If the given value is positive,
     * it is directly set; otherwise, the decimal place count is set to 0.
     *
     * @param lastDecimalPlaces the number of decimal places to set, if positive; otherwise 0.
     */
    void lastDecimalPlaces(int lastDecimalPlaces);

    /**
     * Returns whether the last parsed number had any digits.
     *
     * @return true if the last parsed number had digits, false otherwise.
     */
    boolean lastNumberHadDigits();

    /**
     * Sets whether the last parsed number had any digits.
     *
     * @param lastNumberHadDigits the new value to set, true if the last parsed number had digits, false otherwise.
     */
    void lastNumberHadDigits(boolean lastNumberHadDigits);

    /**
     * Skips over characters in the byte string until a terminating character is encountered.
     *
     * @param tester the StopCharTester instance to use for determining the terminating character.
     * @return true if a terminating character was found, false if the end of the buffer was reached.
     * @throws IllegalStateException if this operation cannot be performed currently.
     */
    default boolean skipTo(@NotNull StopCharTester tester)
            throws IllegalStateException {
        return BytesInternal.skipTo(this, tester);
    }

    /**
     * Parses text from the byte string as a BigDecimal.
     *
     * @return the parsed BigDecimal.
     * @throws IllegalStateException if this operation cannot be performed currently.
     * @throws ArithmeticException   if a numerical overflow occurs during the operation.
     */
    @NotNull
    default BigDecimal parseBigDecimal()
            throws IllegalStateException, ArithmeticException {
        return new BigDecimal(parseUtf8(StopCharTesters.NUMBER_END));
    }
}
