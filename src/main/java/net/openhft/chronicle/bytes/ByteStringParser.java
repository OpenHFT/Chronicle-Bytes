/*
 * Copyright 2016 higherfrequencytrading.com
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
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.core.annotation.Nullable;

import java.io.Reader;
import java.math.BigDecimal;
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
    default Boolean parseBoolean(@NotNull StopCharTester tester) {
        return BytesInternal.parseBoolean(this, tester);
    }

    @Nullable
    default Boolean parseBoolean() {
        return BytesInternal.parseBoolean(this, StopCharTesters.NON_ALPHA_DIGIT);
    }

    /**
     * parse text with UTF-8 decoding as character terminated.
     *
     * @param stopCharTester to check if the end has been reached.
     * @return the text as a String.
     */
    @NotNull
    @ForceInline
    default String parseUtf8(@NotNull StopCharTester stopCharTester) {
        return BytesInternal.parseUtf8(this, stopCharTester);
    }

    @NotNull
    @Deprecated
    default String parseUTF(@NotNull StopCharTester stopCharTester) {
        return parseUtf8(stopCharTester);
    }

    /**
     * parse text with UTF-8 decoding as character terminated.
     *
     * @param buffer         to populate
     * @param stopCharTester to check if the end has been reached.
     */
    @ForceInline
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester) throws BufferUnderflowException {
        BytesInternal.parseUtf8(this, buffer, stopCharTester);
    }

    @Deprecated
    default void parseUTF(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester) throws BufferUnderflowException {
        parseUtf8(buffer, stopCharTester);
    }

    /**
     * parse text with UTF-8 decoding as one or two character terminated.
     *
     * @param buffer          to populate
     * @param stopCharsTester to check if the end has been reached.
     */
    @ForceInline
    default void parseUtf8(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, IORuntimeException {
        BytesInternal.parseUtf8(this, buffer, stopCharsTester);
    }

    @Deprecated
    default void parseUTF(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException, IORuntimeException {
        parseUtf8(buffer, stopCharsTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param buffer         to populate
     * @param stopCharTester to check if the end has been reached.
     */
    @SuppressWarnings("rawtypes")
    @ForceInline
    default void parse8bit(Appendable buffer, @NotNull StopCharTester stopCharTester)
            throws BufferUnderflowException {
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
            throws BufferUnderflowException {
        return BytesInternal.parse8bit(this, stopCharTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param buffer          to populate
     * @param stopCharsTester to check if the end has been reached.
     */
    @SuppressWarnings("rawtypes")
    @ForceInline
    default void parse8bit(Appendable buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharsTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharsTester);
    }

    @SuppressWarnings("rawtypes")
    default void parse8bit(Bytes buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    default void parse8bit(StringBuilder buffer, @NotNull StopCharsTester stopCharsTester)
            throws BufferUnderflowException {
        BytesInternal.parse8bit(this, buffer, stopCharsTester);
    }

    /**
     * parse text as an int. The terminating character is consumed.
     *
     * @return an int.
     */
    @ForceInline
    default int parseInt() throws BufferUnderflowException {
        return Maths.toInt32(BytesInternal.parseLong(this));
    }

    /**
     * parse text as a long integer. The terminating character is consumed.
     *
     * @return a long.
     */
    @ForceInline
    default long parseLong() throws BufferUnderflowException {
        return BytesInternal.parseLong(this);
    }

    /**
     * parse text as a float decimal. The terminating character is consumed.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return a float.
     */
    default float parseFloat() throws BufferUnderflowException {
        return (float) BytesInternal.parseDouble(this);
    }

    /**
     * parse text as a double decimal. The terminating character is consumed.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return a double.
     */
    default double parseDouble() throws BufferUnderflowException {
        return BytesInternal.parseDouble(this);
    }

    /**
     * Parse the significant digits of a decimal number.
     * <p>
     * The number of decimal places can be retrieved with  lastDecimalPlaces()
     *
     * @return the significant digits
     */
    default long parseLongDecimal() throws BufferUnderflowException {
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
    @ForceInline
    default boolean skipTo(@NotNull StopCharTester tester) {
        return BytesInternal.skipTo(this, tester);
    }

    @NotNull
    default BigDecimal parseBigDecimal() {
        return new BigDecimal(parseUtf8(StopCharTesters.NUMBER_END));
    }

}
