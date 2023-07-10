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

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * The {@code RandomDataOutput} interface provides a set of methods for writing data to a buffer
 * or similar data structure at arbitrary positions (offsets). It includes methods for writing
 * all primitive types, as well as arrays, {@link CharSequence}s and other types of data.
 *
 * <p>Writing methods in this interface often include parameters for specifying the offset at
 * which to start writing, as well as the data to be written. These methods can be used for
 * low-level operations such as directly writing to memory regions or disk blocks.
 *
 * <p>In addition to methods for writing individual items, this interface also includes methods
 * for copying data from one buffer to another or from arrays.
 *
 * <p>This interface also provides atomic and volatile write operations to ensure thread safety
 * and visibility when accessing and manipulating data in a concurrent environment.
 *
 * <p>Methods in this interface may throw {@link BufferOverflowException} if the requested
 * operation would exceed the buffer's current capacity or {@link IllegalStateException} if the
 * buffer has been previously released.
 *
 * @see RandomDataInput
 */

@SuppressWarnings({"rawtypes", "unchecked"})
public interface RandomDataOutput<R extends RandomDataOutput<R>> extends RandomCommon {
    /**
     * Writes a byte value at the specified offset.
     *
     * @param offset The position within the data stream to write the byte to.
     * @param i      The byte value to write. Must be within the range of a byte (-128 to 127).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException  If the specified offset exceeds the available capacity.
     * @throws IllegalArgumentException If the provided integer value cannot be safely cast to a byte without loss of information.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeByte(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, ArithmeticException, IllegalStateException {
        return writeByte(offset, Maths.toInt8(i));
    }

    /**
     * Writes an unsigned byte value at the specified offset.
     *
     * @param offset The position within the data stream to write the unsigned byte to.
     * @param i      The unsigned byte value to write. Must be within the range of an unsigned byte (0 to 255).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException  If the specified offset exceeds the available capacity.
     * @throws IllegalArgumentException If the provided integer value cannot be safely cast to an unsigned byte without loss of information.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeUnsignedByte(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, ArithmeticException, IllegalStateException {
        return writeByte(offset, (byte) Maths.toUInt8(i));
    }

    /**
     * Writes a boolean value at the specified offset.
     *
     * @param offset The position within the data stream to write the boolean value to.
     * @param flag   The boolean value to write. Translates 'true' as 'Y' and 'false' as 'N'.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeBoolean(@NonNegative long offset, boolean flag)
            throws BufferOverflowException, IllegalStateException {
        try {
            return writeByte(offset, flag ? 'Y' : 'N');

        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Writes an unsigned short value at the specified offset.
     *
     * @param offset The position within the data stream to write the unsigned short to.
     * @param i      The unsigned short value to write. Must be within the range of an unsigned short (0 to 65535).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws ArithmeticException     If the provided integer value cannot be safely cast to an unsigned short without loss of information.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeUnsignedShort(@NonNegative long offset, int i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeShort(offset, (short) Maths.toUInt16(i));
    }

    /**
     * Writes an unsigned integer value at the specified offset.
     *
     * @param offset The position within the data stream to write the unsigned integer to.
     * @param i      The unsigned integer value to write. Must be within the range of an unsigned integer (0 to 4294967295).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws ArithmeticException     If the provided long value cannot be safely cast to an unsigned integer without loss of information.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeUnsignedInt(@NonNegative long offset, long i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        return writeInt(offset, (int) Maths.toUInt32(i));
    }

    /**
     * Writes a byte at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the byte to.
     * @param i8     The byte value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeByte(@NonNegative long offset, byte i8)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a short integer at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the short integer to.
     * @param i      The short integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeShort(@NonNegative long offset, short i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a 24-bit integer at the specified non-negative offset. This method writes the lower 16 bits
     * and then the upper 8 bits of the integer in two steps.
     *
     * @param offset The non-negative position within the data stream to write the 24-bit integer to.
     * @param i      The integer value to write. Only the lowest 24 bits are used.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset plus two exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeInt24(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        writeShort(offset, (short) i);
        return writeByte(offset + 2, (byte) (i >> 16));
    }

    /**
     * Writes an integer value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the integer to.
     * @param i      The integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeInt(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes an integer at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the integer to.
     * @param i      The integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeOrderedInt(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes a floating-point number at the specified offset.
     *
     * @param offset The position within the data stream to write the float to.
     * @param f      The float value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeOrderedFloat(@NonNegative long offset, float f)
            throws BufferOverflowException, IllegalStateException {
        return writeOrderedInt(offset, Float.floatToRawIntBits(f));
    }

    /**
     * Writes a long integer value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the long integer to.
     * @param i      The long integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeLong(@NonNegative long offset, long i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes a long integer at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the long integer to.
     * @param i      The long integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeOrderedLong(@NonNegative long offset, long i)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes a double-precision floating-point number at the specified offset.
     *
     * @param offset The position within the data stream to write the double to.
     * @param d      The double value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeOrderedDouble(@NonNegative long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        return writeOrderedLong(offset, Double.doubleToRawLongBits(d));
    }

    /**
     * Writes a single-precision floating-point value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the float to.
     * @param d      The float value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeFloat(@NonNegative long offset, float d)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a double-precision floating-point value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the double to.
     * @param d      The double value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeDouble(@NonNegative long offset, double d)
            throws BufferOverflowException, IllegalStateException;
    /**
     * Writes a volatile byte at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the byte to.
     * @param i8     The byte value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeVolatileByte(@NonNegative long offset, byte i8)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a volatile short at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the short to.
     * @param i16    The short value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeVolatileShort(@NonNegative long offset, short i16)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a volatile integer at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the integer to.
     * @param i32    The integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeVolatileInt(@NonNegative long offset, int i32)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a volatile long integer at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the long integer to.
     * @param i64    The long integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    R writeVolatileLong(@NonNegative long offset, long i64)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes a volatile single-precision floating-point value at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the float to.
     * @param f      The float value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeVolatileFloat(@NonNegative long offset, float f)
            throws BufferOverflowException, IllegalStateException {
        return writeVolatileInt(offset, Float.floatToRawIntBits(f));
    }

    /**
     * Writes a volatile double-precision floating-point value at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the double to.
     * @param d      The double value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException If the specified offset exceeds the available capacity.
     * @throws IllegalStateException    if released
     */
    @NotNull
    default R writeVolatileDouble(@NonNegative long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        return writeVolatileLong(offset, Double.doubleToRawLongBits(d));
    }

    /**
     * Copies the entire byte array into this data output. This is a convenience method for {@link #write(long, byte[], int, int)}.
     *
     * @param offsetInRDO the non-negative offset within the data output where the byte array should be written.
     * @param bytes       the byte array to be written.
     * @return a reference to this instance.
     * @throws BufferOverflowException  if the capacity of this data output was exceeded.
     * @throws IllegalStateException    if this data output has been previously released.
     * @throws NullPointerException     if the provided byte array is null.
     */
    @NotNull
    default R write(@NonNegative long offsetInRDO, byte[] bytes)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(bytes);
        return write(offsetInRDO, bytes, 0, bytes.length);
    }

    /**
     * Copies the provided {@code byteArray} to this Bytes object starting at {@code writeOffset} taking
     * content starting at {@code readOffset} but copying at most {@code length} bytes.
     * <p>
     * Does not update cursors e.g. {@link #writePosition}
     *
     * @param writeOffset the non-negative offset within the data output where the segment should be written.
     * @param byteArray   the byte array containing the segment to be written.
     * @param readOffset  the non-negative offset within the byte array where the segment begins.
     * @param length      the non-negative length of the segment.
     * @return a reference to this instance.
     * @throws BufferOverflowException  if the capacity of this data output was exceeded.
     * @throws IllegalStateException    if this data output has been previously released.
     * @throws IllegalArgumentException if any of the provided offsets or length are negative.
     * @throws NullPointerException     if the provided byte array is null.
     */
    @NotNull
    R write(@NonNegative long writeOffset,
            byte[] byteArray,
            @NonNegative int readOffset,
            @NonNegative int length) throws BufferOverflowException, IllegalStateException;

    /**
     * Copies a segment from the provided ByteBuffer into this data output.
     * <p>
     * Does not update cursors e.g. {@link #writePosition}
     *
     * @param writeOffset the non-negative offset within the data output where the segment should be written.
     * @param bytes       the ByteBuffer containing the segment to be written.
     * @param readOffset  the non-negative offset within the ByteBuffer where the segment begins.
     * @param length      the non-negative length of the segment.
     * @throws BufferOverflowException if the capacity of this data output was exceeded.
     * @throws IllegalStateException   if this data output has been previously released.
     */
    void write(@NonNegative long writeOffset, @NotNull ByteBuffer bytes, @NonNegative int readOffset, @NonNegative int length)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Copies the entire content of the provided BytesStore into this data output. This is a convenience method for {@link #write(long, RandomDataInput, long, long)}.
     *
     * @param offsetInRDO the non-negative offset within the data output where the BytesStore content should be written.
     * @param bytes       the BytesStore whose content should be written.
     * @return a reference to this instance.
     * @throws BufferOverflowException  if the capacity of this data output was exceeded.
     * @throws IllegalStateException    if this data output has been previously released.
     * @throws NullPointerException     if the provided BytesStore is null.
     */
    @NotNull
    default R write(@NonNegative long offsetInRDO, @NotNull BytesStore bytes)
            throws BufferOverflowException, IllegalStateException {
        requireNonNull(bytes);
        try {
            return write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());

        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Copies a segment from the provided RandomDataInput into this data output.
     * This operation does not update the {@link #writePosition} of this output nor the {@link RandomDataInput#readPosition()} of the input.
     *
     * @param writeOffset the non-negative offset within this data output where the segment should be written.
     * @param bytes       the RandomDataInput source containing the segment to be written.
     * @param readOffset  the non-negative offset within the source where the segment begins.
     * @param length      the non-negative length of the segment.
     * @return a reference to this instance.
     * @throws BufferOverflowException if the capacity of this data output was exceeded.
     * @throws BufferUnderflowException if the source does not have enough data to fill the length.
     * @throws IllegalStateException if this data output has been previously released.
     */
    @NotNull
    R write(@NonNegative long writeOffset, @NotNull RandomDataInput bytes, @NonNegative long readOffset, @NonNegative long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException;

    /**
     * Fills the specified range in this data output with zeroes.
     *
     * @param start the starting index of the range to zero out (inclusive).
     * @param end   the ending index of the range to zero out (exclusive).
     * @return a reference to this instance.
     * @throws IllegalStateException if this data output has been previously released.
     */
    @NotNull
    R zeroOut(@NonNegative long start, @NonNegative long end)
            throws IllegalStateException;

    /**
     * Appends a long value as a string with a specified number of digits at the given offset.
     *
     * @param offset the non-negative offset to append the string at.
     * @param value  the long value to be appended.
     * @param digits the number of digits in the appended string.
     * @return a reference to this instance.
     * @throws BufferOverflowException if the capacity of this data output was exceeded.
     * @throws IllegalArgumentException if the number of digits is not compatible with the long value.
     * @throws IllegalStateException if this data output has been previously released.
     */
    @NotNull
    default R append(@NonNegative long offset, long value, int digits)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        BytesInternal.append(this, offset, value, digits);
        return (R) this;
    }

    /**
     * Appends a double value as a string with a specified number of decimal places and total digits at the given offset.
     *
     * @param offset        the non-negative offset to append the string at.
     * @param value         the double value to be appended.
     * @param decimalPlaces the number of decimal places in the appended string.
     * @param digits        the total number of digits in the appended string.
     * @return a reference to this instance.
     * @throws BufferOverflowException if the capacity of this data output was exceeded.
     * @throws IllegalArgumentException if the number of digits or decimal places is not compatible with the double value.
     * @throws IllegalStateException if this data output has been previously released.
     * @throws ArithmeticException if rounding errors occur during the conversion of the double value to string.
     */
    @NotNull
    default R append(@NonNegative long offset, double value, int decimalPlaces, int digits)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException, ArithmeticException {
        if (decimalPlaces < 20) {
            double d2 = value * Maths.tens(decimalPlaces);
            if (d2 <= Long.MAX_VALUE && d2 >= Long.MIN_VALUE) {
                BytesInternal.appendDecimal(this, Math.round(d2), offset, decimalPlaces, digits);
                return (R) this;
            }
        }
        BytesInternal.append((StreamingDataOutput) this, value);
        return (R) this;
    }

    /**
     * Expert-level method that copies data directly from native memory into this BytesStore.
     *
     * @param address  the address in the native memory from where data should be copied.
     * @param position the position in the BytesStore where data should be written.
     * @param size     the size of the data, in bytes, to be copied from the native memory.
     * @throws BufferOverflowException if the capacity of this BytesStore was exceeded.
     * @throws IllegalStateException if this BytesStore has been previously released.
     */
    void nativeWrite(long address, @NonNegative long position, @NonNegative long size)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Writes the provided {@code text} into this {@code RandomDataOutput} writing at the given {@code writeOffset},
     * in Utf8 format. Returns the new write position after writing the provided {@code text}.
     *
     * @param writeOffset the offset at which the text should be written.
     * @param text        the CharSequence to write, which can be null.
     * @return the offset after the text has been written.
     * @throws BufferOverflowException if the capacity of this RandomDataOutput was exceeded.
     * @throws IllegalStateException if this RandomDataOutput has been previously released.
     * @throws ArithmeticException if errors occur during the conversion of the CharSequence to UTF-8.
     * @see RandomDataInput#readUtf8(long, Appendable)
     */
    default long writeUtf8(@NonNegative long writeOffset, @Nullable CharSequence text)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.writeUtf8(this, writeOffset, text);
    }

    /**
     * Writes the given {@code text} to this {@code RandomDataOutput} writing at the provided {@code writeOffset},
     * in Utf8 format, checking that the utf8 encoding size of the given char sequence is less or
     * equal to the provided {@code maxUtf8Len}, otherwise {@code IllegalArgumentException} is thrown,
     * and no bytes of this {@code RandomDataOutput} are overwritten. Returns the new write position after
     * writing the provided {@code text}
     *
     * @param writeOffset the offset at which the text should be written.
     * @param text        the CharSequence to write, which can be null.
     * @param maxUtf8Len  the maximum length of the UTF-8 encoded text.
     * @return the offset after the text has been written.
     * @throws IllegalArgumentException if the UTF-8 encoding size of the text exceeds maxUtf8Len.
     * @throws BufferOverflowException if the capacity of this RandomDataOutput was exceeded.
     * @throws IllegalStateException if this RandomDataOutput has been previously released.
     * @throws ArithmeticException if errors occur during the conversion of the CharSequence to UTF-8.
     * @see RandomDataInput#readUtf8Limited(long, Appendable, int)
     * @see RandomDataInput#readUtf8Limited(long, int)
     */
    default long writeUtf8Limited(@NonNegative long writeOffset, @Nullable CharSequence text, @NonNegative int maxUtf8Len)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.writeUtf8(this, writeOffset, text, maxUtf8Len);
    }

    /**
     * Writes a BytesStore instance to this RandomDataOutput at the given position.
     * The length of the BytesStore content is encoded using a stop bit encoding scheme.
     *
     * @param position the position at which the BytesStore content should be written.
     * @param bs       the BytesStore instance to write.
     * @return the offset after the BytesStore has been written.
     */
    long write8bit(@NonNegative long position, @NotNull BytesStore bs);
    /**
     * Writes a portion of a string to this RandomDataOutput at the given position.
     * The length of the string is encoded using a stop bit encoding scheme.
     *
     * @param position the position at which the string should be written.
     * @param s        the string to write.
     * @param start    the starting index from where characters are to be taken from the string.
     * @param length   the number of characters to be written from the string.
     * @return the offset after the string has been written.
     */
    long write8bit(@NonNegative long position, @NotNull String s, @NonNegative int start, @NonNegative int length);


    /**
     * Performs a 32-bit compare-and-swap (CAS) operation at a given offset.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful, false otherwise.
     * @throws BufferOverflowException if the capacity of this RandomDataOutput was exceeded.
     * @throws IllegalStateException if this RandomDataOutput has been previously released.
     */
    boolean compareAndSwapInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Tests if the current value at the specified offset equals the expected value and, if so, sets it to the provided value.
     *
     * @param offset   the offset at which to perform the test-and-set operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @throws BufferOverflowException if the capacity of this RandomDataOutput was exceeded.
     * @throws IllegalStateException if this RandomDataOutput has been previously released.
     */
    void testAndSetInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException;


    /**
     * Performs a 64-bit compare-and-swap (CAS) operation at a given offset.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful, false otherwise.
     * @throws BufferOverflowException if the capacity of this RandomDataOutput was exceeded.
     * @throws IllegalStateException if this RandomDataOutput has been previously released.
     */
    boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException;


    /**
     * Performs a compare-and-swap (CAS) operation for a 32-bit float at the given offset. The CAS
     * operation is atomic, meaning that it will compare the current value at the specified offset
     * with the expected value and, if they are equal, it will set the value to the new one.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful (the value was updated), false otherwise.
     * @throws BufferOverflowException if the offset plus the size of a float exceeds the buffer's capacity.
     * @throws IllegalStateException if the buffer has been previously released.
     */
    boolean compareAndSwapFloat(@NonNegative long offset, float expected, float value);

    /**
     * Performs a compare-and-swap (CAS) operation for a 64-bit double at the given offset. Similar to the
     * {@link #compareAndSwapFloat(long, float, float)}, this is an atomic operation.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful (the value was updated), false otherwise.
     * @throws BufferOverflowException if the offset plus the size of a double exceeds the buffer's capacity.
     * @throws IllegalStateException if the buffer has been previously released.
     */
    boolean compareAndSwapDouble(@NonNegative long offset, double expected, double value);

    /**
     * Atomically adds a 32-bit integer value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, which means that it's performed as
     * a single, uninterruptible unit.
     *
     * @param offset the offset at which the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException if the specified offset is not within the bounds of the buffer.
     * @throws IllegalStateException if the buffer has been previously released.
     */
    int addAndGetInt(@NonNegative long offset, int adding);

    /**
     * Atomically adds a 64-bit long value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, meaning it's performed as a single,
     * uninterruptible unit.
     *
     * @param offset the offset where the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException if the specified offset is not within the bounds of the buffer.
     * @throws IllegalStateException if the buffer has been previously released.
     */
    long addAndGetLong(@NonNegative long offset, long adding);

    /**
     * Atomically adds a 32-bit float value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, which implies that it's performed as a
     * single, uninterruptible operation.
     *
     * @param offset the offset at which the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException if the specified offset is not within the bounds of the buffer.
     * @throws IllegalStateException if the buffer has been previously released.
     */
    float addAndGetFloat(@NonNegative long offset, float adding);

    /**
     * Atomically adds a 64-bit double value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, which ensures that it's performed as a
     * single, uninterruptible unit.
     *
     * @param offset the offset where the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException if the specified offset is not within the bounds of the buffer.
     * @throws IllegalStateException if the buffer has been previously released.
     */
    double addAndGetDouble(@NonNegative long offset, double adding);

    default long appendAndReturnLength(long writePosition, boolean negative, long mantissa, int exponent, boolean append0) {
        throw new UnsupportedOperationException();
    }

    @Deprecated(/* to be removed in x.25 */)
    // internal method to cache a byte[]
    byte[] internalNumberBuffer();
}
