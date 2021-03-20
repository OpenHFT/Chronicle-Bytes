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
interface ByteStringParser<B extends ByteStringParser<B>> extends StreamingDataInput<B> {
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
     * Return true or false, or null if it could not be detected
     * as true or false.  Case is not important
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

    @NotNull
    @Deprecated(/* to be removed in x.22 */)
    default String parseUTF(@NotNull StopCharTester stopCharTester)
            throws IllegalStateException, ArithmeticException {
        return parseUtf8(stopCharTester);
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

    @Deprecated(/* to be removed in x.22 */)
    default void parseUTF(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        parseUtf8(buffer, stopCharTester);
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

    @Deprecated(/* to be removed in x.22 */)
    default void parseUTF(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        parseUtf8(buffer, stopCharsTester);
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
     * parse text as a float decimal. The terminating character is consumed.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return a float.
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
     * @return a double.
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
