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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
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
 * operation would exceed the buffer's current capacity or {@link ClosedIllegalStateException} if the
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
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws IllegalArgumentException       If the provided integer value cannot be safely cast to a byte without loss of information.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeByte(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException {
        return writeByte(offset, Maths.toInt8(i));
    }

    /**
     * Writes an unsigned byte value at the specified offset.
     *
     * @param offset The position within the data stream to write the unsigned byte to.
     * @param i      The unsigned byte value to write. Must be within the range of an unsigned byte (0 to 255).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws IllegalArgumentException       If the provided integer value cannot be safely cast to an unsigned byte without loss of information.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeUnsignedByte(@NonNegative long offset, int i)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException {
        return writeByte(offset, (byte) Maths.toUInt8(i));
    }

    /**
     * Writes a boolean value at the specified offset.
     *
     * @param offset The position within the data stream to write the boolean value to.
     * @param flag   The boolean value to write. Translates 'true' as 'Y' and 'false' as 'N'.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeBoolean(@NonNegative long offset, boolean flag)
            throws BufferOverflowException, ClosedIllegalStateException {
        return writeByte(offset, flag ? 'Y' : 'N');
    }

    /**
     * Writes an unsigned short value at the specified offset.
     *
     * @param offset The position within the data stream to write the unsigned short to.
     * @param i      The unsigned short value to write. Must be within the range of an unsigned short (0 to 65535).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ArithmeticException            If the provided integer value cannot be safely cast to an unsigned short without loss of information.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeUnsignedShort(@NonNegative long offset, int i)
            throws BufferOverflowException, ArithmeticException, ClosedIllegalStateException {
        return writeShort(offset, (short) Maths.toUInt16(i));
    }

    /**
     * Writes an unsigned integer value at the specified offset.
     *
     * @param offset The position within the data stream to write the unsigned integer to.
     * @param i      The unsigned integer value to write. Must be within the range of an unsigned integer (0 to 4294967295).
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ArithmeticException            If the provided long value cannot be safely cast to an unsigned integer without loss of information.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeUnsignedInt(@NonNegative long offset, long i)
            throws BufferOverflowException, ArithmeticException, ClosedIllegalStateException {
        return writeInt(offset, (int) Maths.toUInt32(i));
    }

    /**
     * Writes a byte at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the byte to.
     * @param i8     The byte value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeByte(@NonNegative long offset, byte i8)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a short integer at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the short integer to.
     * @param i      The short integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeShort(@NonNegative long offset, short i)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a 24-bit integer at the specified non-negative offset. This method writes the lower 16 bits
     * and then the upper 8 bits of the integer in two steps.
     *
     * @param offset The non-negative position within the data stream to write the 24-bit integer to.
     * @param i      The integer value to write. Only the lowest 24 bits are used.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset plus two exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeInt24(@NonNegative long offset, int i)
            throws BufferOverflowException, ClosedIllegalStateException {
        writeShort(offset, (short) i);
        return writeByte(offset + 2, (byte) (i >> 16));
    }

    /**
     * Writes an integer value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the integer to.
     * @param i      The integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeInt(@NonNegative long offset, int i)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes an integer at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the integer to.
     * @param i      The integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeOrderedInt(@NonNegative long offset, int i)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes a floating-point number at the specified offset.
     *
     * @param offset The position within the data stream to write the float to.
     * @param f      The float value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeOrderedFloat(@NonNegative long offset, float f)
            throws BufferOverflowException, ClosedIllegalStateException {
        return writeOrderedInt(offset, Float.floatToRawIntBits(f));
    }

    /**
     * Writes a long integer value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the long integer to.
     * @param i      The long integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeLong(@NonNegative long offset, long i)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes a long integer at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the long integer to.
     * @param i      The long integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeOrderedLong(@NonNegative long offset, long i)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Performs a non-blocking write operation with a memory barrier to ensure order of stores.
     * Writes a double-precision floating-point number at the specified offset.
     *
     * @param offset The position within the data stream to write the double to.
     * @param d      The double value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeOrderedDouble(@NonNegative long offset, double d)
            throws BufferOverflowException, ClosedIllegalStateException {
        return writeOrderedLong(offset, Double.doubleToRawLongBits(d));
    }

    /**
     * Writes a single-precision floating-point value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the float to.
     * @param d      The float value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeFloat(@NonNegative long offset, float d)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a double-precision floating-point value at the specified non-negative offset.
     *
     * @param offset The non-negative position within the data stream to write the double to.
     * @param d      The double value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeDouble(@NonNegative long offset, double d)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a volatile byte at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the byte to.
     * @param i8     The byte value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeVolatileByte(@NonNegative long offset, byte i8)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a volatile short at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the short to.
     * @param i16    The short value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeVolatileShort(@NonNegative long offset, short i16)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a volatile integer at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the integer to.
     * @param i32    The integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeVolatileInt(@NonNegative long offset, int i32)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a volatile long integer at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the long integer to.
     * @param i64    The long integer value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R writeVolatileLong(@NonNegative long offset, long i64)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Writes a volatile single-precision floating-point value at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the float to.
     * @param f      The float value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeVolatileFloat(@NonNegative long offset, float f)
            throws BufferOverflowException, ClosedIllegalStateException {
        return writeVolatileInt(offset, Float.floatToRawIntBits(f));
    }

    /**
     * Writes a volatile double-precision floating-point value at the specified non-negative offset. The write is volatile, ensuring it is not cached and instantly visible to all threads.
     *
     * @param offset The non-negative position within the data stream to write the double to.
     * @param d      The double value to write.
     * @return Reference to the current instance, allowing for method chaining.
     * @throws BufferOverflowException        If the specified offset exceeds the available capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R writeVolatileDouble(@NonNegative long offset, double d)
            throws BufferOverflowException, ClosedIllegalStateException {
        return writeVolatileLong(offset, Double.doubleToRawLongBits(d));
    }

    /**
     * Copies the entire byte array into this data output. This is a convenience method for {@link #write(long, byte[], int, int)}.
     *
     * @param offsetInRDO the non-negative offset within the data output where the byte array should be written.
     * @param bytes       the byte array to be written.
     * @return a reference to this instance.
     * @throws BufferOverflowException        If the capacity of this data output was exceeded.
     * @throws NullPointerException           If the provided byte array is null.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R write(@NonNegative long offsetInRDO, byte[] bytes)
            throws BufferOverflowException, ClosedIllegalStateException {
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
     * @throws BufferOverflowException        If the capacity of this data output was exceeded.
     * @throws IllegalArgumentException       If any of the provided offsets or length are negative.
     * @throws NullPointerException           If the provided byte array is null.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R write(@NonNegative long writeOffset,
            byte[] byteArray,
            @NonNegative int readOffset,
            @NonNegative int length) throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Copies a segment from the provided ByteBuffer into this data output.
     * <p>
     * Does not update cursors e.g. {@link #writePosition}
     *
     * @param writeOffset the non-negative offset within the data output where the segment should be written.
     * @param bytes       the ByteBuffer containing the segment to be written.
     * @param readOffset  the non-negative offset within the ByteBuffer where the segment begins.
     * @param length      the non-negative length of the segment.
     * @throws BufferOverflowException        If the capacity of this data output was exceeded.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    void write(@NonNegative long writeOffset, @NotNull ByteBuffer bytes, @NonNegative int readOffset, @NonNegative int length)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Copies the entire content of the provided BytesStore into this data output. This is a convenience method for {@link #write(long, RandomDataInput, long, long)}.
     *
     * @param offsetInRDO the non-negative offset within the data output where the BytesStore content should be written.
     * @param bytes       the BytesStore whose content should be written.
     * @return a reference to this instance.
     * @throws BufferOverflowException     If the capacity of this data output was exceeded.
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     * @throws NullPointerException        If the provided BytesStore is null.
     */
    @NotNull
    default R write(@NonNegative long offsetInRDO, @NotNull BytesStore bytes)
            throws BufferOverflowException, ClosedIllegalStateException {
        requireNonNull(bytes);
        return write(offsetInRDO, bytes, bytes.readPosition(), bytes.readRemaining());
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
     * @throws BufferOverflowException        If the capacity of this data output was exceeded.
     * @throws BufferUnderflowException       If the source does not have enough data to fill the length.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R write(@NonNegative long writeOffset, @NotNull RandomDataInput bytes, @NonNegative long readOffset, @NonNegative long length)
            throws BufferOverflowException, BufferUnderflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Fills the specified range in this data output with zeroes.
     *
     * @param start the starting index of the range to zero out (inclusive).
     * @param end   the ending index of the range to zero out (exclusive).
     * @return a reference to this instance.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    R zeroOut(@NonNegative long start, @NonNegative long end)
            throws ClosedIllegalStateException;

    /**
     * Appends a long value as a string with a specified number of digits at the given offset.
     *
     * @param offset the non-negative offset to append the string at.
     * @param value  the long value to be appended.
     * @param digits the number of digits in the appended string.
     * @return a reference to this instance.
     * @throws BufferOverflowException        If the capacity of this data output was exceeded.
     * @throws IllegalArgumentException       If the number of digits is not compatible with the long value.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R append(@NonNegative long offset, long value, int digits)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException {
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
     * @throws BufferOverflowException     If the capacity of this data output was exceeded.
     * @throws IllegalArgumentException    If the number of digits or decimal places is not compatible with the double value.
     * @throws ArithmeticException         If rounding errors occur during the conversion of the double value to string.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    default R append(@NonNegative long offset, double value, int decimalPlaces, int digits)
            throws BufferOverflowException, IllegalArgumentException, ClosedIllegalStateException, ArithmeticException, ThreadingIllegalStateException {
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
     * @throws BufferOverflowException        If the capacity of this BytesStore was exceeded.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    void nativeWrite(long address, @NonNegative long position, @NonNegative long size)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes the provided {@code text} into this {@code RandomDataOutput} writing at the given {@code writeOffset},
     * in Utf8 format. Returns the new write position after writing the provided {@code text}.
     *
     * @param writeOffset the offset at which the text should be written.
     * @param text        the CharSequence to write, which can be null.
     * @return the offset after the text has been written.
     * @throws BufferOverflowException     If the capacity of this RandomDataOutput was exceeded.
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     * @throws ArithmeticException         If errors occur during the conversion of the CharSequence to UTF-8.
     * @see RandomDataInput#readUtf8(long, Appendable)
     */
    default long writeUtf8(@NonNegative long writeOffset, @Nullable CharSequence text)
            throws BufferOverflowException, ClosedIllegalStateException, ArithmeticException {
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
     * @throws IllegalArgumentException    If the UTF-8 encoding size of the text exceeds maxUtf8Len.
     * @throws BufferOverflowException     If the capacity of this RandomDataOutput was exceeded.
     * @throws ClosedIllegalStateException If the resource has been released or closed.
     * @throws ArithmeticException         If errors occur during the conversion of the CharSequence to UTF-8.
     * @see RandomDataInput#readUtf8Limited(long, Appendable, int)
     * @see RandomDataInput#readUtf8Limited(long, int)
     */
    default long writeUtf8Limited(@NonNegative long writeOffset, @Nullable CharSequence text, @NonNegative int maxUtf8Len)
            throws BufferOverflowException, ClosedIllegalStateException, ArithmeticException {
        return BytesInternal.writeUtf8(this, writeOffset, text, maxUtf8Len);
    }

    /**
     * Writes a BytesStore instance to this RandomDataOutput at the given position.
     * The length of the BytesStore content is encoded using a stop bit encoding scheme.
     *
     * @param position the position at which the BytesStore content should be written.
     * @param bs       the BytesStore instance to write.
     * @return the offset after the BytesStore has been written.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long write8bit(@NonNegative long position, @NotNull BytesStore bs) throws ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Writes a portion of a string to this RandomDataOutput at the given position.
     * The length of the string is encoded using a stop bit encoding scheme.
     *
     * @param position the position at which the string should be written.
     * @param s        the string to write.
     * @param start    the starting index from where characters are to be taken from the string.
     * @param length   the number of characters to be written from the string.
     * @return the offset after the string has been written.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long write8bit(@NonNegative long position, @NotNull String s, @NonNegative int start, @NonNegative int length)
            throws ClosedIllegalStateException, ThreadingIllegalStateException;


    /**
     * Performs a 32-bit compare-and-swap (CAS) operation at a given offset.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful, false otherwise.
     * @throws BufferOverflowException        If the capacity of this RandomDataOutput was exceeded.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    boolean compareAndSwapInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, ClosedIllegalStateException;

    /**
     * Tests if the current value at the specified offset equals the expected value and, if so, sets it to the provided value.
     *
     * @param offset   the offset at which to perform the test-and-set operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @throws BufferOverflowException        If the capacity of this RandomDataOutput was exceeded.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    void testAndSetInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, ClosedIllegalStateException;


    /**
     * Performs a 64-bit compare-and-swap (CAS) operation at a given offset.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful, false otherwise.
     * @throws BufferOverflowException        If the capacity of this RandomDataOutput was exceeded.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException;


    /**
     * Performs a compare-and-swap (CAS) operation for a 32-bit float at the given offset. The CAS
     * operation is atomic, meaning that it will compare the current value at the specified offset
     * with the expected value and, if they are equal, it will set the value to the new one.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful (the value was updated), false otherwise.
     * @throws BufferOverflowException        If the offset plus the size of a float exceeds the buffer's capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    boolean compareAndSwapFloat(@NonNegative long offset, float expected, float value) throws ClosedIllegalStateException;

    /**
     * Performs a compare-and-swap (CAS) operation for a 64-bit double at the given offset. Similar to the
     * {@link #compareAndSwapFloat(long, float, float)}, this is an atomic operation.
     *
     * @param offset   the offset at which to perform the CAS operation.
     * @param expected the expected current value.
     * @param value    the new value to set if the current value matches the expected value.
     * @return true if the CAS operation was successful (the value was updated), false otherwise.
     * @throws BufferOverflowException        If the offset plus the size of a double exceeds the buffer's capacity.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    boolean compareAndSwapDouble(@NonNegative long offset, double expected, double value) throws ClosedIllegalStateException;

    /**
     * Atomically adds a 32-bit integer value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, which means that it's performed as
     * a single, uninterruptible unit.
     *
     * @param offset the offset at which the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException       If the specified offset is not within the bounds of the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    int addAndGetInt(@NonNegative long offset, int adding) throws ClosedIllegalStateException;

    /**
     * Atomically adds a 64-bit long value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, meaning it's performed as a single,
     * uninterruptible unit.
     *
     * @param offset the offset where the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException       If the specified offset is not within the bounds of the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    long addAndGetLong(@NonNegative long offset, long adding) throws ClosedIllegalStateException;

    /**
     * Atomically adds a 32-bit float value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, which implies that it's performed as a
     * single, uninterruptible operation.
     *
     * @param offset the offset at which the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException       If the specified offset is not within the bounds of the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    float addAndGetFloat(@NonNegative long offset, float adding) throws ClosedIllegalStateException;

    /**
     * Atomically adds a 64-bit double value to the current value at the specified offset and
     * returns the resulting sum. This operation is atomic, which ensures that it's performed as a
     * single, uninterruptible unit.
     *
     * @param offset the offset where the current value is stored and to which the specified
     *               value is to be added.
     * @param adding the value to add to the current value at the specified offset.
     * @return the sum of the original value at the specified offset and the value being added.
     * @throws BufferUnderflowException       If the specified offset is not within the bounds of the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    double addAndGetDouble(@NonNegative long offset, double adding) throws ClosedIllegalStateException;

    default long appendAndReturnLength(long writePosition, boolean negative, long mantissa, int exponent, boolean append0) {
        throw new UnsupportedOperationException();
    }
}
