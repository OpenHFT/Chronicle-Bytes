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

import net.openhft.chronicle.bytes.internal.ByteStringWriter;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.render.Decimaliser;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.io.Writer;
import java.math.BigDecimal;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * This interface provides methods for appending different types of data to an underlying buffer. The data is appended in the form of bytes.
 * The interface also extends the StreamingDataOutput and Appendable interfaces, thus inheriting their methods.
 *
 * @param <B> the type that extends ByteStringAppender
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
     * Appends a UTF-8 encoded character to the buffer.
     *
     * @param ch the character to append
     * @return the ByteStringAppender instance with the appended character
     * @throws BufferOverflowException        If the append operation exceeds the buffer's capacity
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    @NotNull
    default B append(char ch)
            throws IllegalStateException {
        BytesInternal.appendUtf8Char(this, ch);
        return (B) this;
    }

    /**
     * Append a characters in UTF-8
     *
     * @param cs the CharSequence to append
     * @return this
     * @throws BufferUnderflowException       If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * Appends a long in decimal with a given number of decimal places. Prints value * 10^-decimalPlaces
     *
     * @param value         to append
     * @param decimalPlaces to shift the decimal place
     * @return this
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
     * Appends a float in decimal notation
     *
     * @param f the float number to append
     * @return this
     * @throws BufferOverflowException        If the relative append operation exceeds the underlying buffer's capacity
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
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
     * Appends a double in decimal notation
     *
     * @param d to append
     * @return this
     * @throws BufferOverflowException        If the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException             If an error occurred while attempting to resize the underlying buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default B append(double d)
            throws BufferOverflowException, IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException {
        Bytes<?> bytes = BytesInternal.acquireBytes();
        bytes.append(d);
        append(bytes);
        return (B) this;
    }

    /**
     * Gets the Decimaliser currently associated with this ByteStringAppender.
     *
     * @return The Decimaliser currently associated with this ByteStringAppender.
     */
    Decimaliser decimaliser();

    /**
     * Associates a Decimaliser with this ByteStringAppender.
     *
     * <p>The Decimaliser is an interface which can be implemented to provide custom logic
     * for rendering decimal numbers in this ByteStringAppender.
     *
     * @param decimaliser The Decimaliser to be associated with this ByteStringAppender.
     * @return The ByteStringAppender instance with the Decimaliser set.
     */
    B decimaliser(Decimaliser decimaliser);

    /**
     * @return whether floating point add .0 to indicate it is a floating point even if redundant.
     */
    boolean fpAppend0();

    /**
     * @param append0 Does floating point add .0 to indicate it is a floating point even if redundant.
     * @return this
     */
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
     * Appends the ISO-8859-1 representation of the specified BytesStore.
     *
     * @param bs the BytesStore to append
     * @return this
     * @throws BufferOverflowException        If the BytesStore is too large to write in the capacity available
     * @throws BufferUnderflowException       If the capacity of the underlying buffer was exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    default B append8bit(@NotNull BytesStore bs)
            throws BufferOverflowException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        return write(bs, 0L, bs.readRemaining());
    }

    /**
     * Appends the ISO-8859-1 representation of the specified String.
     *
     * @param cs the String to append
     * @return this
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
    default B append8bit(@NotNull BytesStore bs, @NonNegative long start, @NonNegative long end)
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

    // internal method to cache a byte[]
    @Deprecated(/* to be removed in x.25 */)
    byte[] internalNumberBuffer();
}
