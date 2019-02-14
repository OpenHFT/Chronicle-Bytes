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
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.annotation.NotNull;

import java.io.Writer;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Methods to append text to a Bytes. This extends the Appendable interface.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface ByteStringAppender<B extends ByteStringAppender<B>> extends StreamingDataOutput<B>, Appendable {

    /**
     * @return these Bytes as a Writer
     */
    @NotNull
    default Writer writer() {
        return new ByteStringWriter(this);
    }

    /**
     * Append a char in UTF-8
     *
     * @param ch to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     */
    @Override
    @NotNull
    default B append(char ch) throws BufferOverflowException {
        BytesInternal.appendUtf8Char(this, ch);
        return (B) this;
    }

    /**
     * Append a characters in UTF-8
     *
     * @param cs to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     */
    @Override
    @NotNull
    default B append(@NotNull CharSequence cs) throws BufferOverflowException {
        try {
            return append(cs, 0, cs.length());
        } catch (IndexOutOfBoundsException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Append a boolean as T or F
     *
     * @param flag to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(boolean flag) throws BufferOverflowException {
        return append(flag ? 'T' : 'F');
    }

    /**
     * Append an int in decimal
     *
     * @param value to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(int value) throws BufferOverflowException {
        BytesInternal.appendBase10(this, value);
        return (B) this;
    }

    /**
     * Append a long in decimal
     *
     * @param value to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(long value) throws BufferOverflowException {
        if (value == (int) value)
            BytesInternal.appendBase10(this, (int) value);
        else
            BytesInternal.appendBase10(this, value);
        return (B) this;
    }

    @NotNull
    default B appendBase(long value, int base) throws BufferOverflowException {
        BytesInternal.append(this, value, base);
        return (B) this;
    }

    @NotNull
    default B appendBase16(long value) throws BufferOverflowException {
        BytesInternal.appendBase16(this, value, 1);
        return (B) this;
    }

    @NotNull
    default B appendBase16(long value, int minDigits) throws BufferOverflowException {
        BytesInternal.appendBase16(this, value, minDigits);
        return (B) this;
    }

    /**
     * Append a long in decimal with a given number of decimal places. Print value * 10^-decimalPlaces
     *
     * @param value         to append
     * @param decimalPlaces to shift the decimal place.
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B appendDecimal(long value, int decimalPlaces) throws BufferOverflowException {
        BytesInternal.appendDecimal(this, value, decimalPlaces);
        return (B) this;
    }

    /**
     * Append a float in decimal notation
     *
     * @param f to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(float f) throws BufferOverflowException {
        float f2 = Math.abs(f);
        if (f2 > 1e6 || f2 < 1e-3) {
            return append(Float.toString(f));
        }
        int precision = (int) Math.floor(6 - Math.log10(f2));
        long tens = Maths.tens(precision);
        return append((double) Math.round(f * tens) / tens);
    }

    /**
     * Append a double in decimal notation
     *
     * @param d to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(double d) throws BufferOverflowException {
        BytesInternal.append(this, d);
        return (B) this;
    }

    /**
     * Append a double in decimal notation to a specific number of decimal places. Trailing zeros are not truncated.
     * <p>
     * If the number would normally be printed with more decimal places, the number is rounded.
     *
     * @param d             to append
     * @param decimalPlaces to always produce
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(double d, int decimalPlaces) throws BufferOverflowException {
        if (decimalPlaces < 0)
            throw new IllegalArgumentException();
        if (decimalPlaces < 20) {
            double d2 = d * Maths.tens(decimalPlaces);
            if (d2 <= Long.MAX_VALUE && d2 >= Long.MIN_VALUE) {
                // changed from java.lang.Math.round(d2) as this was shown up to cause latency
                long round = d2 > 0.0 ? (long) (d2 + 0.5) : (long) (d2 - 0.5);
                return appendDecimal(round, decimalPlaces);
            }
        }
        return append(d);
    }

    /**
     * Append a portion of a String to the Bytes in UTF-8.
     *
     * @param cs    to copy
     * @param start index of the first char inclusive
     * @param end   index of the last char exclusive.
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     */
    @Override
    @NotNull
    default B append(@NotNull CharSequence cs, int start, int end)
            throws IndexOutOfBoundsException, BufferOverflowException {
        BytesInternal.appendUtf8(this, cs, start, end - start);
        return (B) this;
    }

    /**
     * Append a String to the Bytes in ISO-8859-1
     *
     * @param cs to write
     * @return this
     * @throws BufferOverflowException  If the string as too large to write in the capacity available
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     */
    @NotNull
    default B append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException {
        return append8bit(cs, 0, cs.length());
    }

    default B append8bit(@NotNull String cs)
            throws BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException {
        return append8bit(cs, 0, cs.length());
    }

    /**
     * Append a portion of a String to the Bytes in ISO-8859-1
     *
     * @param cs    to copy
     * @param start index of the first char inclusive
     * @param end   index of the last char exclusive.
     * @return this
     * @throws BufferOverflowException   If the string as too large to write in the capacity available
     * @throws BufferUnderflowException  if the capacity of the underlying buffer was exceeded
     * @throws IndexOutOfBoundsException if the start or the end are not valid for the CharSequence
     */
    default B append8bit(@NotNull CharSequence cs, int start, int end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs, (long) start, end);
        }
        for (int i = start; i < end; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeByte((byte) c);
        }
        return (B) this;
    }

    @NotNull
    default B appendDateMillis(long dateInMillis) {
        BytesInternal.appendDateMillis(this, dateInMillis);
        return (B) this;
    }

    @NotNull
    default B appendTimeMillis(long timeOfDayInMillis) {
        BytesInternal.appendTimeMillis(this, timeOfDayInMillis % 86400_000L);
        return (B) this;
    }

    @NotNull
    default B append(@NotNull BigDecimal bigDecimal) {
        append(bigDecimal.toString());
        return (B) this;
    }
}
