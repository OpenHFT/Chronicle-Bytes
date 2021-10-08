/*
 * Copyright 2016-2020 chronicle.software
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
 * Supports parsing bytes as text.  You can parse them as special or white space terminated text.
 */
public interface ByteStringParser<B extends ByteStringParser<B>> extends StreamingDataInput<B> {
    /**
     * Access these bytes as an ISO-8859-1 encoded Reader
     *
     * @return as a Reader
     */
    @NotNull
    default Reader reader() {
        return new ByteStringReader(this);
    }

    /**
     * Return <code>true</code> or false, or null if it could not be detected
     * as <code>true</code> or false.  Case is not important
     * <p>
     * <p>false: f, false, n, no, 0
     * <p>
     * <p>true: t, true, y, yes, 1
     *
     * @param tester to detect the end of the text.
     * @return true, false, or null if neither.
     */
    @Nullable
    default Boolean parseBoolean(@NotNull StopCharTester tester)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.parseBoolean(this, tester);
    }

    @Nullable
    default Boolean parseBoolean()
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.parseBoolean(this, StopCharTesters.NON_ALPHA_DIGIT);
    }

    /**
     * parse text with UTF-8 decoding as character terminated.
     *
     * @param stopCharTester to check if the end has been reached.
     * @return the text as a String.
     */
    @NotNull
    default String parseUtf8(@NotNull StopCharTester stopCharTester)
            throws IllegalStateException, ArithmeticException {
        return BytesInternal.parseUtf8(this, stopCharTester);
    }

    /**
     * parse text with UTF-8 decoding as character terminated.
     *
     * @param buffer         to populate
     * @param stopCharTester to check if the end has been reached.
     */
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        BytesInternal.parseUtf8(this, buffer, stopCharTester);
    }

    /**
     * parse text with UTF-8 decoding as one or two character terminated.
     *
     * @param buffer          to populate
     * @param stopCharsTester to check if the end has been reached.
     */
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        BytesInternal.parseUtf8(this, buffer, stopCharsTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param buffer         to populate
     * @param stopCharTester to check if the end has been reached.
     */
    @SuppressWarnings("rawtypes")
    default void parse8bit(Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException, ArithmeticException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param stopCharTester to check if the end has been reached.
     */
    default String parse8bit(@NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parse8bit(this, stopCharTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param buffer          to populate
     * @param stopCharsTester to check if the end has been reached.
     */
    @SuppressWarnings("rawtypes")
    default void parse8bit(Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException, ArithmeticException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharsTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharsTester);
    }

    @SuppressWarnings("rawtypes")
    default void parse8bit(Bytes buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException, ArithmeticException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    default void parse8bit(StringBuilder buffer, @NotNull StopCharsTester stopCharsTester)
            throws IllegalStateException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    /**
     * parse text as an int. The terminating character is consumed.
     *
     * @return an int.
     */
    default int parseInt()
            throws BufferUnderflowException, ArithmeticException, IllegalStateException {
        return Maths.toInt32(BytesInternal.parseLong(this));
    }

    /**
     * parse text as a long integer. The terminating character is consumed.
     *
     * @return a long.
     */
    default long parseLong()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseLong(this);
    }

    /**
     * Parses a long in regular or scientific format.
     * In case text corresponds to a valid long value, returns it without precision errors
     * (unlike result of {@link #parseDouble()} casted to long).
     *
     * @throws IORuntimeException if text parses to a fractional number or to a number outside of the long's range.
     * @return a long.
     */
    default long parseFlexibleLong()
            throws BufferUnderflowException, IllegalStateException, IORuntimeException {
        return BytesInternal.parseFlexibleLong(this);
    }

    /**
     * parse text as a float decimal. The terminating character is consumed.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return a float  or -0.0 if there were no digits present
     */
    default float parseFloat()
            throws BufferUnderflowException, IllegalStateException {
        return (float) BytesInternal.parseDouble(this);
    }

    /**
     * parse text as a double decimal. The terminating character is consumed.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return a double or -0.0 if there were no digits present
     */
    default double parseDouble()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseDouble(this);
    }

    /**
     * Parse the significant digits of a decimal number.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return the significant digits
     */
    default long parseLongDecimal()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseLongDecimal(this);
    }

    /**
     * @return the last number of decimal places for parseDouble or parseLongDecimal
     */
    int lastDecimalPlaces();

    /**
     * Store the last number of decimal places. If
     *
     * @param lastDecimalPlaces set the number of decimal places if positive, otherwise 0.
     */
    void lastDecimalPlaces(int lastDecimalPlaces);

    /**
     * @return the last number had digits
     */
    boolean lastNumberHadDigits();

    /**
     * @param lastNumberHadDigits set the last number had digits
     */
    void lastNumberHadDigits(boolean lastNumberHadDigits);

    /**
     * Skip text until a terminating character is reached.
     *
     * @param tester to stop at
     * @return true if a terminating character was found, false if the end of the buffer was reached.
     */
    default boolean skipTo(@NotNull StopCharTester tester)
            throws IllegalStateException {
        return BytesInternal.skipTo(this, tester);
    }

    @NotNull
    default BigDecimal parseBigDecimal()
            throws IllegalStateException, ArithmeticException {
        return new BigDecimal(parseUtf8(StopCharTesters.NUMBER_END));
    }
}
