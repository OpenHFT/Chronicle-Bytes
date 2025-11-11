/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.ByteStringReader;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Extends {@link StreamingDataInput} with helpers for parsing textual data directly from a
 * {@link Bytes} stream.  Implementations consume data from the current read position and advance it
 * as characters or numbers are parsed.
 *
 * @param <B> self type for fluent chaining
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
     * Parses a boolean token, stopping when {@code tester} returns {@code true}. The recognised
     * tokens are case-insensitive variants of {@code true/t/yes/y/1} and {@code false/f/no/n/0}.
     *
     * @param tester stop condition for the parse
     * @return parsed value or {@code null} if no recognised token was found
     */
    @Nullable
    default Boolean parseBoolean(@NotNull StopCharTester tester)
            throws BufferUnderflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parseBoolean(this, tester);
    }

    /**
     * Convenience wrapper for {@link #parseBoolean(StopCharTester)} using
     * {@link StopCharTesters#NON_ALPHA_DIGIT}.
     */
    @Nullable
    default Boolean parseBoolean()
            throws BufferUnderflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parseBoolean(this, StopCharTesters.NON_ALPHA_DIGIT);
    }

    /**
     * Parses a UTF-8 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition.
     *
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @return the parsed text as a {@code String}.
     * @throws ArithmeticException            If a numeric overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default String parseUtf8(@NotNull StopCharTester stopCharTester)
            throws ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parseUtf8(this, stopCharTester);
    }

    /**
     * Parses a UTF-8 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer         the {@code Appendable} to append the parsed string to.
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ArithmeticException            If a numeric overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.parseUtf8(this, buffer, stopCharTester);
    }

    /**
     * Parses a UTF-8 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code Appendable} to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws IORuntimeException             If an I/O error occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, IORuntimeException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.parseUtf8(this, buffer, stopCharsTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer         the {@code Appendable} to append the parsed string to.
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws BufferOverflowException        If the buffer's capacity was exceeded.
     * @throws ArithmeticException            If a numeric overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default void parse8bit(Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, BufferOverflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharTester);
        else
            BytesInternal.parse8bit(this, (Bytes<?>) buffer, stopCharTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharTester}
     * detects an end condition.
     *
     * @param stopCharTester a {@code StopCharTester} used to detect the end of the string.
     * @return the parsed text as a {@code String}.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default String parse8bit(@NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parse8bit(this, stopCharTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code Appendable} to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws BufferOverflowException        If the buffer's capacity was exceeded.
     * @throws ArithmeticException            If a numeric overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default void parse8bit(Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, BufferOverflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharsTester);
        else
            BytesInternal.parse8bit(this, (Bytes<?>) buffer, stopCharsTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code Bytes} object to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws BufferOverflowException        If the buffer's capacity was exceeded.
     * @throws ArithmeticException            If a numeric overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default void parse8bit(Bytes<?> buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, BufferOverflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    /**
     * Parses an ISO-8859-1 encoded string from the byte string until the provided {@code stopCharsTester}
     * detects an end condition. The parsed string is appended to the provided {@code buffer}.
     *
     * @param buffer          the {@code StringBuilder} to append the parsed string to.
     * @param stopCharsTester a {@code StopCharsTester} used to detect the end of the string.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default void parse8bit(StringBuilder buffer, @NotNull StopCharsTester stopCharsTester)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    /**
     * Parses text from the byte string as an integer. The terminating character is consumed.
     *
     * @return the parsed integer.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ArithmeticException            If a numeric overflow occurs.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default int parseInt()
            throws BufferUnderflowException, ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return Maths.toInt32(BytesInternal.parseLong(this));
    }

    /**
     * Parses text from the byte string as a long integer. The terminating character is consumed.
     *
     * @return the parsed long.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default long parseLong()
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parseLong(this);
    }

    /**
     * Parses a long integer from the byte string in either standard or scientific notation. If the parsed value
     * corresponds to a valid long, it returns the value without any precision errors (unlike casting the result
     * of {@link #parseDouble()} to long).
     *
     * @return the parsed long.
     * @throws IORuntimeException             If the parsed value corresponds to a fractional number or to a number outside the long's range.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default long parseFlexibleLong()
            throws BufferUnderflowException, IORuntimeException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parseFlexibleLong(this);
    }

    /**
     * Parses text from the byte string as a floating-point number. The terminating character is consumed.
     * The number of decimal places can be retrieved with {@code lastDecimalPlaces()}.
     *
     * @return the parsed float, or -0.0 if there were no digits.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default float parseFloat()
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return (float) BytesInternal.parseDouble(this);
    }

    /**
     * Parses text from the byte string as a double-precision floating-point number. The terminating character is consumed.
     * The number of decimal places can be retrieved with {@code lastDecimalPlaces()}.
     *
     * @return the parsed double, or -0.0 if there were no digits.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default double parseDouble()
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.parseDouble(this);
    }

    /**
     * Parses the significant digits of a decimal number from the byte string.
     * The number of decimal places can be retrieved with {@code lastDecimalPlaces()}.
     *
     * @return the significant digits as a long.
     * @throws BufferUnderflowException       If there is insufficient data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default long parseLongDecimal()
            throws BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
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
     * Advances the read position until {@code tester.isStopChar()} returns {@code true} or end of input
     * is reached.  The terminating character itself remains unread.
     */
    default boolean skipTo(@NotNull StopCharTester tester)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        return BytesInternal.skipTo(this, tester);
    }

    /**
     * Parses text from the byte string as a BigDecimal.
     *
     * @return the parsed BigDecimal.
     * @throws ArithmeticException            If a numerical overflow occurs during the operation.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default BigDecimal parseBigDecimal()
            throws ArithmeticException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return new BigDecimal(parseUtf8(StopCharTesters.NUMBER_END));
    }
}
