/*
 * Copyright 2016-2025 chronicle.software
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

import net.openhft.chronicle.bytes.internal.ByteStringWriter;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.render.Decimaliser;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Extension of {@link StreamingDataOutput} and {@link Appendable} that exposes
 * convenience methods for writing text and numbers to a {@link Bytes} stream.
 * Each method returns {@code this} to allow fluent call chains.
 *
 * @param <B> concrete type for fluent chaining
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface ByteStringAppender<B extends ByteStringAppender<B>> extends StreamingDataOutput<B>, Appendable {

    /**
     * Returns the current ByteStringAppender instance as a Writer.
     *
     * @return Writer object representing the ByteStringAppender
     */
    @NotNull
    default Writer writer() {
        return new ByteStringWriter(this);
    }

    /**
     * Appends the given character, typically encoded as UTF-8.
     *
     * @param ch character to append
     * @return this appender
     * @throws BufferOverflowException        if no space is available
     * @throws ClosedIllegalStateException    if the appender has been closed
     * @throws ThreadingIllegalStateException if accessed from multiple threads unsafely
     */
    @Override
    @NotNull
    default B append(char ch)
            throws IllegalStateException {
        BytesInternal.appendUtf8Char(this, ch);
        return (B) this;
    }

    /**
     * Appends the supplied character sequence, encoded as UTF-8.
     *
     * @param cs text to append
     * @return this appender
     * @throws BufferUnderflowException       if the buffer cannot resize
     * @throws ClosedIllegalStateException    if closed
     * @throws ThreadingIllegalStateException if accessed concurrently
     */
    @Override
    @NotNull
    default B append(@NotNull CharSequence cs) {
        if (cs.length() == 0)
            return (B) this;
        return append(cs, 0, cs.length());
    }

    /**
     * Appends a boolean as 'T' or 'F' character.
     *
     * @param flag to append
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IORuntimeException             If an error occurred while attempting to resize the underlying buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B append(boolean flag)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return append(flag ? 'T' : 'F');
    }

    /**
     * Appends an int in decimal to this.
     *
     * @param value the integer value to append
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IORuntimeException             If an error occurred while attempting to resize the underlying buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B append(int value)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.appendBase10(this, value);
        return (B) this;
    }

    /**
     * Appends a long value in decimal.
     *
     * @param value the long number to append
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IORuntimeException             If an error occurred while attempting to resize the underlying buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B append(long value)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        if (value == (int) value)
            BytesInternal.appendBase10(this, (int) value);
        else
            BytesInternal.appendBase10(this, value);
        return (B) this;
    }

    /**
     * Appends a string representation of the first argument in the radix specified by the second argument.
     *
     * @param value the number to append
     * @param base  the radix that the specified value should be converted to before append
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IllegalArgumentException       If the specified arguments are illegal
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B appendBase(long value, int base)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
        if (base == 10)
            append(value);
        else
            BytesInternal.append(this, value, base);
        return (B) this;
    }

    /**
     * Appends the base 16 (hexadecimal) representation of the specified long value.
     *
     * @param value the long value to be converted to base 16 and appended
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IllegalArgumentException       If the specified argument is illegal
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B appendBase16(long value)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.appendBase16(this, value, 1);
        return (B) this;
    }

    /**
     * Appends the base 16 (hexadecimal) representation of the specified long value,
     * padding with leading zeroes if the number of digits is less than minDigits.
     *
     * @param value     the long value to be converted to base 16 and appended
     * @param minDigits the minimum number of digits to be appended
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IllegalArgumentException       If the specified argument is illegal
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B appendBase16(long value, int minDigits)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        BytesInternal.appendBase16(this, value, minDigits);
        return (B) this;
    }

    /**
     * Appends {@code value} as a decimal number with {@code decimalPlaces} digits after the decimal point.
     * For example {@code appendDecimal(12345, 2)} appends {@code "123.45"}.
     *
     * @param value         number scaled by {@code 10^decimalPlaces}
     * @param decimalPlaces number of decimal digits to output
     * @return this appender
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IORuntimeException             If an error occurred while attempting to resize the underlying buffer
     * @throws IllegalArgumentException       If the decimalPlaces is negative or too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B appendDecimal(long value, int decimalPlaces)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        BytesInternal.appendDecimal(this, value, decimalPlaces);
        return (B) this;
    }

    /**
     * Appends {@code f} using the configured {@link Decimaliser}.  Very small or
     * large values may fall back to {@link Float#toString()}.
     */
    @NotNull
    default B append(float f)
            throws BufferOverflowException, IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException {
        float f2 = Math.abs(f);
        if (f2 > 1e6 || f2 < 1e-3) {
            return append(Float.toString(f));
        }
        int precision = (int) Math.floor(6 - Math.log10(f2));
        long tens = Maths.tens(precision);
        return append((double) Math.round(f * tens) / tens);
    }

    /**
     * Appends {@code d} using the current {@link Decimaliser} strategy.
     */
    @NotNull
    default B append(double d)
            throws BufferOverflowException, IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException {
        try (ScopedResource<Bytes<?>> stlBytes = BytesInternal.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            bytes.append(d);
            append(bytes);
            return (B) this;
        }
    }

    /**
     * Returns the strategy used to convert floating point values to text.
     */
    Decimaliser decimaliser();

    /**
     * Sets the {@link Decimaliser} controlling how floating point numbers are rendered.
     *
     * @param decimaliser implementation to use
     * @return this appender
     */
    B decimaliser(Decimaliser decimaliser);

    /**
     * Whether a trailing {@code .0} is appended to whole floating point values.
     *
     * @deprecated to be removed in x.28.  Use {@link #decimaliser(Decimaliser)} to control formatting.
     */
    @Deprecated(/* to remove in x.28 */)
    boolean fpAppend0();

    /**
     * Controls whether a trailing {@code .0} is appended to whole floating point values.
     *
     * @deprecated to be removed in x.28.  Use {@link #decimaliser(Decimaliser)} instead.
     */
    @Deprecated(/* to remove in x.28 */)
    B fpAppend0(boolean append0);

    /**
     * Appends a double in decimal notation to a specific number of decimal places. Trailing zeros are not truncated.
     * <p>
     * If the number would normally be printed with more decimal places, the number is rounded.
     *
     * @param d             to append
     * @param decimalPlaces to always produce
     * @return this
     * @throws BufferOverflowException        If the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException             If an error occurred while attempting to resize the underlying buffer
     * @throws IllegalArgumentException       If the decimalPlaces is negative or too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B append(double d, int decimalPlaces)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.append(this, d, decimalPlaces);
        return (B) this;
    }

    /**
     * Appends a portion of a string to the Bytes in UTF-8.
     *
     * @param cs    the CharacterSequence to append
     * @param start index of the first char inclusive
     * @param end   index of the last char exclusive
     * @return this
     * @throws BufferOverflowException        If the capacity of the underlying buffer was exceeded
     * @throws IndexOutOfBoundsException      If the specified indexes are out of range
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @Override
    @NotNull
    default B append(@NotNull CharSequence cs, @NonNegative int start, @NonNegative int end)
            throws IndexOutOfBoundsException {
        BytesInternal.appendUtf8(this, cs, start, end - start);
        return (B) this;
    }

    /**
     * Appends a String to the Bytes in ISO-8859-1.
     *
     * @param cs the CharSequence to append
     * @return this
     * @throws BufferOverflowException        If the string is too large to write in the capacity available
     * @throws BufferUnderflowException       If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return append8bit(cs, 0, cs.length());
    }

    /**
     * Appends the content of {@code bs} assuming one byte per character (ISO-8859-1).
     * Characters outside the 8-bit range are replaced with '?'.
     *
     * @param bs the BytesStore to append
     * @return this appender
     * @throws BufferOverflowException        If the BytesStore is too large to write in the capacity available
     * @throws BufferUnderflowException       If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default B append8bit(@NotNull BytesStore<?, ?> bs)
            throws BufferOverflowException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return write(bs, 0L, bs.readRemaining());
    }

    /**
     * Appends {@code cs} in 8-bit encoding (ISO-8859-1). Characters above 255 become '?'.
     *
     * @param cs the String to append
     * @return this appender
     * @throws BufferOverflowException        If the string is too large to write in the capacity available
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default B append8bit(@NotNull String cs)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return append8bit(cs, 0, cs.length());
    }

    /**
     * Appends a portion of a string to this Bytes in ISO-8859-1.
     *
     * @param cs    the CharSequence to append
     * @param start index of the first char of cs (inclusive) to append
     * @param end   index of the last char of cs (exclusive) to append
     * @return this
     * @throws BufferOverflowException        If the string is too large to write in the capacity available
     * @throws BufferUnderflowException       If the capacity of the underlying buffer was exceeded
     * @throws IndexOutOfBoundsException      If the start or the end are not valid for the CharSequence
     * @throws IllegalArgumentException       If the start or end is negative or too large
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default B append8bit(@NotNull CharSequence cs, @NonNegative int start, @NonNegative int end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException, ClosedIllegalStateException, ThreadingIllegalStateException {
        assert end >= start : "end=" + end + ",start=" + start;
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs, (long) start, end - start);
        }
        for (int i = start; i < end; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeByte((byte) c);
        }
        return (B) this;
    }

    /**
     * Appends a portion of a BytesStore to this in ISO-8859-1 format.
     *
     * @param bs    the specified BytesStore that a portion of it will be appended to this
     * @param start the index of first byte (inclusive) of bs to append
     * @param end   the number of bytes of bs to append
     * @return this
     * @throws IllegalArgumentException       If an illegal argument is passed to the method
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws BufferUnderflowException       If the capacity of the BytesStore was exceeded
     * @throws IndexOutOfBoundsException      If the specified indexes for the BytesStore are out of range
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default B append8bit(@NotNull BytesStore<?, ?> bs, @NonNegative long start, @NonNegative long end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException, ClosedIllegalStateException, ThreadingIllegalStateException {
        assert end > start : "end=" + end + ",start=" + start;
        return write(bs, start, end - start);
    }

    /**
     * Converts a specified long number to a date in the format yyyymmdd and appends the date to this.
     * The specified long number represents a point in time that is time milliseconds after January 1, 1970 00:00:00 GMT.
     *
     * @param dateInMillis the specified long to convert to date and append to this
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B appendDateMillis(long dateInMillis)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.appendDateMillis(this, dateInMillis);
        return (B) this;
    }

    /**
     * Converts a specified long number to time of day and appends it to this. The specified long number
     * represents time in milliseconds after 00:00:00.000 GMT which will be converted to hours, minutes, seconds and milliseconds.
     * <p>
     * Twelve bytes in the format of hh:mm:ss.ddd will be appended to this. hh, mm, ss and ddd represent
     * hour, minute, second and millisecond.
     *
     * @param timeOfDayInMillis the long number that represents time of day in milliseconds
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws IllegalArgumentException       If an illegal argument is passed to the method
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B appendTimeMillis(long timeOfDayInMillis)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException, ThreadingIllegalStateException {
        BytesInternal.appendTimeMillis(this, timeOfDayInMillis % 86400_000L);
        return (B) this;
    }

    /**
     * Appends a string representation of a specified BigDecimal to this.
     * <p>
     * The string representation of the BigDecimal number is a standard canonical string form as
     * described in {@link BigDecimal#toString()}.
     *
     * @param bigDecimal the specified BigDecimal to append
     * @return this
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     * @see java.math.BigDecimal
     */
    @NotNull
    default B append(@NotNull BigDecimal bigDecimal)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        append(bigDecimal.toString());
        return (B) this;
    }
}
