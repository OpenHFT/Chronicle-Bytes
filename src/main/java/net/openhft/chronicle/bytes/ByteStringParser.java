/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;

/**
 * Supports parsing bytes as text.  You can parse them as special or white space terminated text.
 */
interface ByteStringParser<B extends ByteStringParser<B>> extends StreamingDataInput<B> {
    /**
     * Access these bytes as an ISO-8859-1 encoded Reader
     *
     * @return as a Reader
     */
    default Reader reader() {
        return new ByteStringReader(this);
    }

    /**
     * parse text with UTF-8 decoding as character terminated.
     *
     * @param stopCharTester to check if the end has been reached.
     * @return the text as a String.
     */
    @NotNull
    @ForceInline
    default String parseUTF(@NotNull StopCharTester stopCharTester) {
        return BytesInternal.parseUTF(this, stopCharTester);
    }

    /**
     * parse text with UTF-8 decoding as character terminated.
     *
     * @param buffer to populate
     * @param stopCharTester to check if the end has been reached.
     */
    @ForceInline
    default void parseUTF(@NotNull Appendable buffer, @NotNull StopCharTester stopCharTester) {
        BytesInternal.parseUTF(this, buffer, stopCharTester);
    }

    /**
     * parse text with UTF-8 decoding as one or two character terminated.
     *
     * @param buffer to populate
     * @param stopCharsTester to check if the end has been reached.
     */
    @ForceInline
    default void parseUTF(@NotNull Appendable buffer, @NotNull StopCharsTester stopCharsTester) {
        BytesInternal.parseUTF(this, buffer, stopCharsTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param buffer to populate
     * @param stopCharTester to check if the end has been reached.
     */
    @ForceInline
    default void parse8bit(Appendable buffer, @NotNull StopCharTester stopCharTester) {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharTester);
    }

    /**
     * parse text with ISO-8859-1 decoding as character terminated.
     *
     * @param buffer to populate
     * @param stopCharsTester to check if the end has been reached.
     */
    @ForceInline
    default void parse8bit(Appendable buffer, @NotNull StopCharsTester stopCharsTester) {
        if (buffer instanceof StringBuilder)
            BytesInternal.parse8bit(this, (StringBuilder) buffer, stopCharsTester);
        else
            BytesInternal.parse8bit(this, (Bytes) buffer, stopCharsTester);
    }

    /**
     * parse text as a long integer. The terminating character is consumed.
     * @return a long.
     */
    @ForceInline
    default long parseLong() {
        return BytesInternal.parseLong(this);
    }

    /**
     * parse text as a double decimal. The terminating character is consumed.
     * @return a double.
     */
    @ForceInline
    default double parseDouble() {
        return BytesInternal.parseDouble(this);
    }

    /**
     * Skip text until a terminating character is reached.
     * @param tester to stop at
     * @return true if a terminating character was found, false if the end of the buffer was reached.
     */
    @ForceInline
    default boolean skipTo(@NotNull StopCharTester tester) {
        return BytesInternal.skipTo(this, tester);
    }
}
