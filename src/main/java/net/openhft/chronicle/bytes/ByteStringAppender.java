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

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.UnsafeText;
import org.jetbrains.annotations.NotNull;

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
    default B append(char ch)
            throws IllegalStateException {
        try {
            BytesInternal.appendUtf8Char(this, ch);
            return (B) this;
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
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
    default B append(@NotNull CharSequence cs) {
        if (cs.length() == 0)
            return (B) this;
        return append(cs, 0, cs.length());
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
    default B append(boolean flag)
            throws BufferOverflowException, IllegalStateException {
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
    default B append(int value)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
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
    default B append(long value)
            throws BufferOverflowException, IllegalStateException {
        if (value == (int) value)
            BytesInternal.appendBase10(this, (int) value);
        else
            BytesInternal.appendBase10(this, value);
        return (B) this;
    }

    @NotNull
    default B appendBase(long value, int base)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException, IndexOutOfBoundsException {
        BytesInternal.append(this, value, base);
        return (B) this;
    }

    @NotNull
    default B appendBase16(long value)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        BytesInternal.appendBase16(this, value, 1);
        return (B) this;
    }

    @NotNull
    default B appendBase16(long value, int minDigits)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
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
    default B appendDecimal(long value, int decimalPlaces)
            throws BufferOverflowException, IllegalStateException, ArithmeticException, IllegalArgumentException {
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
    default B append(float f)
            throws BufferOverflowException, IllegalStateException {
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
    default B append(double d)
            throws BufferOverflowException, IllegalStateException {
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
    default B append(double d, int decimalPlaces)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException, ArithmeticException {
        if (decimalPlaces < 0)
            throw new IllegalArgumentException();
        if (decimalPlaces < 18) {
            long factor = Maths.tens(decimalPlaces);
            double d1 = d;
            boolean neg = d1 < 0;
            d1 = Math.abs(d1);
            final double df = d1 * factor;
            if (df < Long.MAX_VALUE) {
                // changed from java.lang.Math.round(d2) as this was shown up to cause latency
                long ldf = (long) df;
                final double residual = df - ldf + Math.ulp(d1) * (factor * 0.983);
                if (residual >= 0.5)
                    ldf++;
                if (neg)
                    ldf = -ldf;
                long round = ldf;

                if (canWriteDirect(20 + decimalPlaces)) {
                    long address = addressForWritePosition();
                    long address2 = UnsafeText.appendBase10d(address, round, decimalPlaces);
                    writeSkip(address2 - address);
                } else {
                    appendDecimal(round, decimalPlaces);
                }
                return (B) this;
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
     * @throws BufferOverflowException if the capacity of the underlying buffer was exceeded
     */
    @Override
    @NotNull
    default B append(@NotNull CharSequence cs, int start, int end)
            throws IndexOutOfBoundsException {
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
            throws BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException, IllegalStateException {
        try {
            return append8bit(cs, 0, cs.length());
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    default B append8bit(@NotNull BytesStore bs)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException {
        try {
            return write(bs, 0L, bs.readRemaining());
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    default B append8bit(@NotNull String cs)
            throws BufferOverflowException, IllegalStateException {
        try {
            return append8bit(cs, 0, cs.length());
        } catch (IllegalArgumentException | IndexOutOfBoundsException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
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
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException, IllegalStateException {
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

    default B append8bit(@NotNull BytesStore bs, long start, long end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException, IllegalStateException {
        return write(bs, start, end);
    }

    @NotNull
    default B appendDateMillis(long dateInMillis)
            throws BufferOverflowException, IllegalStateException {
        BytesInternal.appendDateMillis(this, dateInMillis);
        return (B) this;
    }

    @NotNull
    default B appendTimeMillis(long timeOfDayInMillis)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        BytesInternal.appendTimeMillis(this, timeOfDayInMillis % 86400_000L);
        return (B) this;
    }

    @NotNull
    default B append(@NotNull BigDecimal bigDecimal) {
        append(bigDecimal.toString());
        return (B) this;
    }
}
