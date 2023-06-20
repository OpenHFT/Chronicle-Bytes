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
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * The {@code RandomDataInput} class provides a series of methods for reading data
 * from various types of inputs. It allows to read data from an input source
 * in a non-sequential manner, i.e., the data can be accessed at any offset.
 *
 * <p>This class supports reading of primitive data types like {@code int},
 * {@code long}, {@code double} etc., as well as more complex data structures
 * like {@code byte[]}, {@code String} and {@code ByteBuffer}. It also provides
 * methods for direct reading from memory and for reading with a load barrier.
 *
 * <p>Furthermore, the {@code RandomDataInput} class provides additional methods for
 * advanced operations like copying data to native memory, finding a specific byte,
 * calculating the hash code of a sequence of bytes, and more.
 *
 * <p>Methods in this class may throw {@code BufferUnderflowException} if the offset
 * specified is outside the limits of the byte sequence or {@code IllegalStateException}
 * if the byte sequence has been released.
 *
 * <p>Note: Implementations of this class are typically not thread-safe. If multiple
 * threads interact with a {@code RandomDataInput} instance concurrently, it must be synchronized
 * externally.
 *
 */
public interface RandomDataInput extends RandomCommon {
    String[] charToString = createCharToString();

    /**
     * Creates a lookup table mapping byte values to their corresponding String representations.
     *
     * @return a lookup table for byte-to-String conversions.
     */
    @NotNull
    static String[] createCharToString() {
        @NotNull String[] charToString = new String[256];
        charToString[0] = "\u0660";
        for (int i = 1; i < 21; i++)
            charToString[i] = Character.toString((char) (i + 0x2487));
        for (int i = ' '; i < 256; i++)
            charToString[i] = Character.toString((char) i);
        for (int i = 21; i < ' '; i++)
            charToString[i] = "\\u00" + Integer.toHexString(i).toUpperCase();
        for (int i = 0x80; i < 0xA0; i++)
            charToString[i] = "\\u00" + Integer.toHexString(i).toUpperCase();
        return charToString;
    }

    /**
     * Reads a volatile int value from the current reading position.
     *
     * @return the read int value.
     * @throws BufferUnderflowException if the reading position is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default int peekVolatileInt()
            throws BufferUnderflowException, IllegalStateException {
        return readVolatileInt(readPosition());
    }

    /**
     * Reads a boolean value from a specific offset.
     *
     * @param offset the location from where the boolean value is read.
     * @return the read boolean value.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default boolean readBoolean(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return BytesUtil.byteToBoolean(readByte(offset));
    }

    /**
     * Reads a byte value from a specific offset.
     *
     * @param offset the location from where the byte value is read.
     * @return the read byte value.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    byte readByte(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads an unsigned byte value from a specific offset.
     * The value is returned as an int in order to represent the unsigned byte as a positive value.
     *
     * @param offset the location from where the unsigned byte value is read.
     * @return the unsigned byte value interpreted as a positive int.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default int readUnsignedByte(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readByte(offset) & 0xFF;
    }

    /**
     * Reads an unsigned byte value from a specific offset.
     * Returns -1 if the byte read is at the end of the byte source.
     * The value is returned as an int in order to represent the unsigned byte as a positive value.
     *
     * @param offset the location from where the unsigned byte value is read.
     * @return the unsigned byte value interpreted as a positive int or -1.
     * @throws IllegalStateException if the byte source has been released.
     */
    int peekUnsignedByte(@NonNegative long offset)
            throws IllegalStateException;

    /**
     * Reads a short value from a specific offset.
     *
     * @param offset the location from where the short value is read.
     * @return the read short value.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    short readShort(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads an unsigned short value from a specific offset.
     * The value is returned as an int in order to represent the unsigned short as a positive value.
     *
     * @param offset the location from where the unsigned short value is read.
     * @return the unsigned short value interpreted as a positive int.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default int readUnsignedShort(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readShort(offset) & 0xFFFF;
    }

    /**
     * Reads an unsigned 24-bit integer value from a specific offset.
     * The value is returned as an int, with the upper 8 bits zeroed.
     *
     * @param offset the location from where the unsigned 24-bit integer value is read.
     * @return the unsigned 24-bit integer value interpreted as a positive int.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default int readUnsignedInt24(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readUnsignedShort(offset) | (readUnsignedByte(offset) << 16);
    }

    /**
     * Reads a 32-bit integer value from a specific offset.
     *
     * @param offset the location from where the 32-bit integer value is read.
     * @return the read int value.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    int readInt(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads an unsigned 32-bit integer value from a specific offset.
     * The value is returned as a long in order to represent the unsigned int as a positive value.
     *
     * @param offset the location from where the unsigned 32-bit integer value is read.
     * @return the unsigned 32-bit integer value interpreted as a positive long.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default long readUnsignedInt(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readInt(offset) & 0xFFFFFFFFL;
    }

    /**
     * Reads a 64-bit long value from a specific offset.
     *
     * @param offset the location from where the long value is read.
     * @return the read long value.
     * @throws BufferUnderflowException if the offset is outside the bounds of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    long readLong(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a 32-bit floating point value from a specified offset.
     *
     * @param offset the location from where the float value is read.
     * @return the read float value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    float readFloat(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a 64-bit floating point value from a specified offset.
     *
     * @param offset the location from where the double value is read.
     * @return the read double value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    double readDouble(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a byte value from a specified offset and converts it into a printable string.
     *
     * @param offset the location from where the byte value is read.
     * @return the byte value in a printable string form.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default String printable(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return charToString[readUnsignedByte(offset)];
    }

    /**
     * Reads a volatile 8-bit byte value from a specified offset. This operation includes a memory
     * barrier that prevents reordering of instructions before and after it.
     *
     * @param offset the location from where the byte value is read.
     * @return the volatile byte value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    byte readVolatileByte(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a volatile 16-bit short value from a specified offset. This operation includes a memory
     * barrier that prevents reordering of instructions before and after it.
     *
     * @param offset the location from where the short value is read.
     * @return the volatile short value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    short readVolatileShort(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a volatile 32-bit integer value from a specified offset. This operation includes a memory
     * barrier that prevents reordering of instructions before and after it.
     *
     * @param offset the location from where the int value is read.
     * @return the volatile int value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    int readVolatileInt(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a volatile 32-bit floating point value from a specified offset. This operation includes a memory
     * barrier that prevents reordering of instructions before and after it.
     *
     * @param offset the location from where the float value is read.
     * @return the volatile float value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default float readVolatileFloat(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return Float.intBitsToFloat(readVolatileInt(offset));
    }

    /**
     * Reads a volatile 64-bit long value from a specified offset. This operation includes a memory
     * barrier that prevents reordering of instructions before and after it.
     *
     * @param offset the location from where the long value is read.
     * @return the volatile long value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    long readVolatileLong(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Reads a volatile 64-bit double value from a specified offset. This operation includes a memory
     * barrier that prevents reordering of instructions before and after it.
     *
     * @param offset the location from where the double value is read.
     * @return the volatile double value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default double readVolatileDouble(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return Double.longBitsToDouble(readVolatileLong(offset));
    }

    /**
     * Parses a long value from a specified offset.
     *
     * @param offset the location from where the long value is read.
     * @return the parsed long value.
     * @throws BufferUnderflowException if the offset is beyond the limits of the byte source.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default long parseLong(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseLong(this, offset);
    }

    /**
     * Expert-level method for transferring data from this byte source to native memory.
     *
     * @param position the starting point within the byte source from which data is copied.
     * @param address  the destination address in native memory.
     * @param size     the number of bytes to transfer.
     * @throws BufferUnderflowException if the specified position or size exceeds the byte source limits.
     * @throws IllegalStateException    if the byte source has been released.
     */
    void nativeRead(@NonNegative long position, long address, @NonNegative long size)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Copies data from this byte source into a byte array. The data is read from {@link RandomCommon#readPosition()}
     * up to {@link RandomCommon#readLimit()}.
     *
     * @param bytes the target byte array to which the data is copied.
     * @return the number of bytes actually copied.
     * @throws BufferUnderflowException if the source's read position or limit is beyond the byte source limits.
     * @throws IllegalStateException    if the byte source has been released.
     */
    default int copyTo(byte[] bytes)
            throws BufferUnderflowException, IllegalStateException {
        requireNonNull(bytes);
        throwExceptionIfReleased(this);
        int len = (int) Math.min(bytes.length, readRemaining());
        long readPosition = readPosition();

        int i = 0;
        for (; i < len - 7; i += 8)
            UnsafeMemory.unsafePutLong(bytes, i, readLong(readPosition + i));
        for (; i < len; i++)
            bytes[i] = readByte(readPosition + i);

        return len;
    }

    /**
     * Copies data from this RandomDataInput to the specified ByteBuffer. The number of copied bytes is the
     * minimum of the remaining bytes to read in this RandomDataInput and the remaining capacity of the ByteBuffer.
     * The data copying starts from the current read position in this RandomDataInput and from the current
     * position in the ByteBuffer. The ByteBuffer's position, limit, or mark are not modified by this operation.
     * Returns the number of bytes copied.
     *
     * @param bb the target ByteBuffer to which the data is copied.
     * @return the number of bytes copied.
     * @throws BufferUnderflowException if the read operation encounters end of the byte source.
     * @throws IllegalStateException if the byte source has been released.
     */
    default int copyTo(@NotNull ByteBuffer bb)
            throws BufferUnderflowException, IllegalStateException {
        requireNonNull(bb);
        throwExceptionIfReleased(this);
        int pos = bb.position();
        int len = (int) Math.min(bb.remaining(), readRemaining());
        long readPosition = readPosition();
        int i;
        for (i = 0; i < len - 7; i += 8)
            bb.putLong(pos + i, readLong(readPosition + i));
        for (; i < len; i++)
            bb.put(pos + i, readByte(readPosition + i));
        return len;
    }

    /**
     * Reads a long value from the specified offset. If less than 8 bytes are available to read,
     * this method pads the high bytes with zeros. If the offset is at or beyond the read limit,
     * this method returns 0L.
     *
     * @param offset the location from where the long value is read.
     * @return the long value, potentially zero-padded.
     * @throws IllegalStateException if the byte source has been released.
     */
    default long readIncompleteLong(@NonNegative long offset)
            throws IllegalStateException {
        long left = readLimit() - offset;
        long l;
        try {
            if (left >= 8)
                return readLong(offset);
            if (left == 4)
                return readInt(offset);
            l = 0;
            for (int i = 0, remaining = (int) left; i < remaining; i++) {
                l |= (long) readUnsignedByte(offset + i) << (i * 8);
            }
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
        return l;
    }

    /**
     * Returns the actual capacity that can be potentially read from this byte source.
     *
     * @return the actual readable capacity.
     */
    @Override
    @NonNegative
    long realCapacity();

    /**
     * Perform an atomic add and get operation for a 32-bit int
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.26 */)
    default int addAndGetInt(@NonNegative long offset, int adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetInt((BytesStore<?, ?>) this, offset, adding);
    }

    /**
     * Perform an atomic add and get operation for a 64-bit long
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.26 */)
    default long addAndGetLong(@NonNegative long offset, long adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetLong((BytesStore<?, ?>) this, offset, adding);
    }

    /**
     * Perform an atomic add and get operation for a 32-bit float
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.26 */)
    default float addAndGetFloat(@NonNegative long offset, float adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetFloat((BytesStore<?, ?>) this, offset, adding);
    }

    /**
     * Perform an atomic add and get operation for a 64-bit double
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.26 */)
    default double addAndGetDouble(@NonNegative long offset, double adding)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.addAndGetDouble((BytesStore<?, ?>) this, offset, adding);
    }

    /**
     * Perform a 32-bit CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.25 */)
    boolean compareAndSwapInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException;

    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.25 */)
    void testAndSetInt(@NonNegative long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a 64-bit CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.25 */)
    boolean compareAndSwapLong(@NonNegative long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException;

    /**
     * Perform a 32-bit float CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.25 */)
    default boolean compareAndSwapFloat(@NonNegative long offset, float expected, float value)
            throws BufferOverflowException, IllegalStateException {
        return compareAndSwapInt(offset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(value));
    }

    /**
     * Perform a 64-bit double CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    @Deprecated(/* Use RandomDataOutput instead, to be removed in x.25 */)
    default boolean compareAndSwapDouble(@NonNegative long offset, double expected, double value)
            throws BufferOverflowException, IllegalStateException {
        return compareAndSwapLong(offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
    }

    /**
     * Returns a new BytesStore that is a subsequence of this byte sequence, starting at the specified index and of the specified length.
     *
     * @param start  the start index, inclusive.
     * @param length the length of the subsequence.
     * @return a new BytesStore instance containing the specified subsequence.
     * @throws BufferUnderflowException if the start index or length are outside the limits of this byte sequence.
     * @throws IllegalStateException if the byte source has been released.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    default BytesStore subBytes(@NonNegative long start, @NonNegative long length)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.subBytes(this, start, length);
    }

    /**
     * Finds the first occurrence of the specified byte in this byte sequence.
     *
     * @param stopByte the byte to be searched for.
     * @return the index of the first occurrence of the byte, or -1 if the byte is not found.
     * @throws IllegalStateException if the byte source has been released.
     */
    default long findByte(byte stopByte)
            throws IllegalStateException {
        return BytesInternal.findByte(this, stopByte);
    }

    /**
     * Truncates {@code sb} (it must be a {@link StringBuilder} or {@link Bytes}) and reads a char
     * sequence from the given {@code offset}, encoded as Utf8, into it. Returns offset <i>after</i>
     * the read Utf8, if a normal char sequence was read, or {@code -1 - offset}, if {@code null}
     * was observed (in this case, {@code sb} is truncated too, but not updated then, by querying
     * {@code sb} only this case is indistinguishable from reading an empty char sequence).
     *
     * @param <T>    buffer type, must be {@code StringBuilder} or {@code Bytes}
     * @param offset the offset in this {@code RandomDataInput} to read char sequence from
     * @param sb     the buffer to read char sequence into (truncated first)
     * @return offset after the normal read char sequence, or -1 - offset, if char sequence is
     * {@code null}
     * @throws IORuntimeException if the reading operation encounters an unexpected error.
     * @throws IllegalArgumentException if the buffer is not a {@code StringBuilder} or {@code Bytes}.
     * @throws BufferUnderflowException if the reading operation encounters the end of the byte source.
     * @throws ArithmeticException if the calculated length of the UTF-8 encoded string is invalid.
     * @throws IllegalStateException if the byte source has been released.
     * @see RandomDataOutput#writeUtf8(long, CharSequence)
     */
    default <T extends Appendable & CharSequence> long readUtf8(@NonNegative long offset, @NotNull T sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException, ArithmeticException, IllegalStateException {
        AppendableUtil.setLength(sb, 0);
        // TODO insert some bounds check here

        long utfLen;
        if ((utfLen = readByte(offset++)) < 0) {
            utfLen &= 0x7FL;
            long b;
            int count = 7;
            while ((b = readByte(offset++)) < 0) {
                utfLen |= (b & 0x7FL) << count;
                count += 7;
            }
            if (b != 0) {
                if (count > 56)
                    throw new IORuntimeException(
                            "Cannot read more than 9 stop bits of positive value");
                utfLen |= (b << count);
            } else {
                if (count > 63)
                    throw new IORuntimeException(
                            "Cannot read more than 10 stop bits of negative value");
                utfLen = ~utfLen;
            }
        }
        if (utfLen == -1)
            return ~offset;
        int len = Maths.toUInt31(utfLen);
        BytesInternal.parseUtf8(this, offset, sb, true, len);
        return offset + utfLen;
    }

    /**
     * Truncates {@code sb} (it must be a {@link StringBuilder} or {@link Bytes}) and reads a char
     * sequence from the given {@code offset}, encoded as Utf8, into it. Returns offset <i>after</i>
     * the read Utf8, if a normal char sequence was read, or {@code -1 - offset}, if {@code null}
     * was observed (in this case, {@code sb} is truncated too, but not updated then, by querying
     * {@code sb} only this case is indistinguishable from reading an empty char sequence). If
     * length of Utf8 encoding of the char sequence exceeds {@code maxUtf8Len},
     * {@code IllegalStateException} is thrown.
     *
     * @param <T>        buffer type, must be {@code StringBuilder} or {@code Bytes}
     * @param offset     the offset in this {@code RandomDataInput} to read char sequence from
     * @param sb         the buffer to read char sequence into (truncated first)
     * @param maxUtf8Len the maximum allowed length of the char sequence in Utf8 encoding
     * @return offset after the normal read char sequence, or -1 - offset, if char sequence is
     * {@code null}
     * @throws IllegalStateException if released
     * @see RandomDataOutput#writeUtf8Limited(long, CharSequence, int)
     */
    default <T extends Appendable & CharSequence> long readUtf8Limited(@NonNegative long offset,
                                                                       @NotNull final T sb,
                                                                       @NonNegative final int maxUtf8Len)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException,
            IllegalStateException {
        AppendableUtil.setLength(sb, 0);
        // TODO insert some bounds check here

        long utfLen;
        if ((utfLen = readByte(offset++)) < 0) {
            utfLen &= 0x7FL;
            long b;
            int count = 7;
            while ((b = readByte(offset++)) < 0) {
                utfLen |= (b & 0x7FL) << count;
                count += 7;
            }
            if (b != 0) {
                if (count > 56)
                    throw new IORuntimeException(
                            "Cannot read more than 9 stop bits of positive value");
                utfLen |= (b << count);
            } else {
                if (count > 63)
                    throw new IORuntimeException(
                            "Cannot read more than 10 stop bits of negative value");
                utfLen = ~utfLen;
            }
        }
        if (utfLen == -1)
            return ~offset;
        if (utfLen > maxUtf8Len)
            throw new IllegalStateException("Attempted to read a char sequence of " +
                    "utf8 size " + utfLen + ", when only " + maxUtf8Len + " allowed");
        BytesInternal.parseUtf8(this, offset, sb, true, (int) utfLen);
        return offset + utfLen;
    }

    /**
     * Reads a char sequence from the given {@code offset}, encoded as Utf8. If length of Utf8
     * encoding of the char sequence exceeds {@code maxUtf8Len}, {@code IllegalStateException}
     * is thrown.
     *
     * @param offset     the offset in this {@code RandomDataInput} to read char sequence from
     * @param maxUtf8Len the maximum allowed length of the char sequence in Utf8 encoding
     * @return the char sequence was read
     * @throws IllegalStateException if released
     * @see RandomDataOutput#writeUtf8Limited(long, CharSequence, int)
     */
    @Nullable
    default String readUtf8Limited(@NonNegative long offset, @NonNegative int maxUtf8Len)
            throws BufferUnderflowException, IORuntimeException, IllegalArgumentException,
            IllegalStateException {
        return BytesInternal.readUtf8(this, offset, maxUtf8Len);
    }

    /**
     * Compares the UTF-8 encoded char sequence, written in this {@code RandomDataInput} at the
     * given offset, with the given char sequence. Returns {@code true}, if they are equal. Both
     * char sequences (encoded in bytes and the given) may be {@code null}.
     *
     * @param offset the offset in this {@code RandomDataInput} where the char sequence to compare
     *               is written
     * @param other  the second char sequence to compare
     * @return {@code true} if two char sequences are equal
     * @throws IllegalStateException if released
     * @throws IORuntimeException    if the contents are not a valid string.
     */
    default boolean compareUtf8(@NonNegative long offset, @Nullable CharSequence other)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return BytesInternal.compareUtf8(this, offset, other);
    }

    default byte[] toByteArray()
            throws IllegalStateException {
        return BytesInternal.toByteArray(this);
    }

    /**
     * Reads a sequence of bytes from the specified offset into a byte array.
     *
     * @param offsetInRDI the offset in the byte sequence from which to start reading.
     * @param bytes the byte array into which the data is read.
     * @param offset the start offset in the byte array at which the data is written.
     * @param length the maximum number of bytes to read.
     * @return the actual number of bytes read into the byte array.
     * @throws IllegalStateException if the byte sequence has been released.
     */
    default long read(@NonNegative long offsetInRDI, byte[] bytes, @NonNegative int offset, @NonNegative int length)
            throws IllegalStateException {
        requireNonNull(bytes);
        try {
            int len = Maths.toUInt31(Math.min(length, requireNonNegative(readLimit() - offsetInRDI)));
            for (int i = 0; i < len; i++)
                bytes[offset + i] = readByte(offsetInRDI + i);
            return len;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Converts the byte sequence into a direct byte buffer.
     *
     * @return a direct ByteBuffer containing the data of this byte sequence.
     * @throws IllegalArgumentException if the byte sequence cannot be converted into a ByteBuffer.
     * @throws ArithmeticException if the calculated size of the ByteBuffer is invalid.
     * @throws IllegalStateException if the byte sequence has been released.
     */
    default ByteBuffer toTemporaryDirectByteBuffer()
            throws IllegalArgumentException, ArithmeticException, IllegalStateException {
        throwExceptionIfReleased(this);
        int len = Maths.toUInt31(readRemaining());
        try {
            ByteBuffer bb = ByteBuffer.allocateDirect(len);
            copyTo(bb);
            bb.clear();
            return bb;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Computes a hash code for a sequence of bytes.
     *
     * @param offset the start offset of the sequence of bytes.
     * @param length the length of the sequence of bytes.
     * @return a hash code value for the specified sequence of bytes.
     * @throws BufferUnderflowException if the specified sequence of bytes extends beyond the limits of this byte sequence.
     * @throws IllegalStateException if the byte sequence has been released.
     */
    default int fastHash(@NonNegative long offset, @NonNegative int length)
            throws BufferUnderflowException, IllegalStateException {
        long hash = 0;
        int i = 0;
        if (length >= 4) {
            hash = readInt(offset + i);
            i += 4;
        }
        for (; i < length - 3; i += 4) {
            hash *= 0x6d0f27bd;
            hash += readInt(offset + i);
        }
        if (i < length - 1) {
            hash *= 0x6d0f27bdL;
            hash += readShort(offset + i);
            i += 2;
        }
        if (i < length)
            hash += readByte(offset + i);
        hash *= 0x855dd4db;
        return (int) (hash ^ (hash >> 32));
    }

    /**
     * Checks if this byte sequence can be read directly from native memory.
     *
     * @return true if the byte sequence can be read directly, false otherwise.
     */
    default boolean canReadDirect() {
        return canReadDirect();
    }

    /**
     * Checks if the specified length of bytes can be read directly from native memory.
     *
     * @param length the number of bytes to check.
     * @return true if the byte sequence is backed by direct memory and the remaining bytes are more than or equal to the specified length, false otherwise.
     */
    default boolean canReadDirect(@NonNegative long length) {
        return isDirectMemory() && readRemaining() >= length;
    }
}
