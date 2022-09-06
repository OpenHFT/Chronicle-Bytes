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
 * This allows random access to the underling bytes.  This instance can be used across threads as it is stateless.
 * The thread safety of the underlying data depends on how the methods are used.
 */
public interface RandomDataInput extends RandomCommon {
    String[] charToString = createCharToString();

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

    default int peekVolatileInt()
            throws BufferUnderflowException, IllegalStateException {
        return readVolatileInt(readPosition());
    }

    /**
     * Read boolean at an offset
     *
     * @param offset to read
     * @return the boolean
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default boolean readBoolean(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return BytesUtil.byteToBoolean(readByte(offset));
    }

    /**
     * Read byte at an offset
     *
     * @param offset to read
     * @return the byte
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    byte readByte(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read an unsigned byte at an offset
     *
     * @param offset to read
     * @return the unsigned byte
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default int readUnsignedByte(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readByte(offset) & 0xFF;
    }

    /**
     * Read an unsigned byte at an offset, or -1
     *
     * @param offset to read
     * @return the unsigned byte or -1
     * @throws IllegalStateException if released
     */
    int peekUnsignedByte(@NonNegative long offset)
            throws IllegalStateException;

    /**
     * Read a short at an offset
     *
     * @param offset to read
     * @return the short
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    short readShort(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read an unsigned short at an offset
     *
     * @param offset to read
     * @return the unsigned short
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default int readUnsignedShort(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readShort(offset) & 0xFFFF;
    }

    /**
     * Read an unsigned int at an offset
     *
     * @param offset to read
     * @return the int
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default int readUnsignedInt24(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readUnsignedShort(offset) | (readUnsignedByte(offset) << 16);
    }

    /**
     * Read an int at an offset
     *
     * @param offset to read
     * @return the int
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    int readInt(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read an unsigned int at an offset
     *
     * @param offset to read
     * @return the unsigned int
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default long readUnsignedInt(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return readInt(offset) & 0xFFFFFFFFL;
    }

    /**
     * Read a long at an offset
     *
     * @param offset to read
     * @return the long
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    long readLong(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a float at an offset
     *
     * @param offset to read
     * @return the float
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    float readFloat(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a double at an offset
     *
     * @param offset to read
     * @return the double
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    double readDouble(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read the byte at an offset and converts it into a printable
     *
     * @param offset to read
     * @return the byte in a printable form.
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default String printable(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return charToString[readUnsignedByte(offset)];
    }

    /**
     * Read a 8-bit byte from memory with a load barrier.
     *
     * @param offset to read
     * @return the byte value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    byte readVolatileByte(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a 16-bit short from memory with a load barrier.
     *
     * @param offset to read
     * @return the short value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    short readVolatileShort(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a 32-bit int from memory with a load barrier.
     *
     * @param offset to read
     * @return the int value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    int readVolatileInt(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a float from memory with a load barrier.
     *
     * @param offset to read
     * @return the float value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default float readVolatileFloat(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return Float.intBitsToFloat(readVolatileInt(offset));
    }

    /**
     * Read a 64-bit long from memory with a load barrier.
     *
     * @param offset to read
     * @return the long value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    long readVolatileLong(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a 64-bit double from memory with a load barrier.
     *
     * @param offset to read
     * @return the double value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default double readVolatileDouble(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return Double.longBitsToDouble(readVolatileLong(offset));
    }

    default long parseLong(@NonNegative long offset)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseLong(this, offset);
    }

    /**
     * expert level method for copying data to native memory.
     *
     * @param position within the ByteStore to copy.
     * @param address  in native memory
     * @param size     in bytes
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    void nativeRead(@NonNegative long position, long address, @NonNegative long size)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * Read a byte[] from memory from {@link RandomCommon#readPosition()}
     * to {@link RandomCommon#readLimit()}.
     *
     * @return the length actually read.
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IllegalStateException    if released
     */
    default int copyTo(byte[] bytes)
            throws BufferUnderflowException, IllegalStateException {
        requireNonNull(bytes);
        throwExceptionIfReleased(this);
        int len = (int) Math.min(bytes.length, readRemaining());
        long readPosition = readPosition();

        for (int i = 0; i < len; i++)
            bytes[i] = readByte(readPosition + i);

        return len;
    }

    /**
     * Copy data from this RandomDataInput to the ByteBuffer. The minimum of {@link #readRemaining()} and
     * {@link ByteBuffer#remaining()}. Starting from {@link #start()} in this RandomDataInput and from {@link
     * ByteBuffer#position()} of the given bb. Does NOT change the position or limit or mark of the given ByteBuffer.
     * Returns the number of the copied bytes.
     *
     * @throws IllegalStateException if released
     */
    default int copyTo(@NotNull ByteBuffer bb)
            throws BufferUnderflowException, IllegalStateException {
        requireNonNull(bb);
        throwExceptionIfReleased(this);
        int pos = bb.position();
        int len = (int) Math.min(bb.remaining(), readRemaining());
        int i;
        for (i = 0; i < len - 7; i += 8)
            bb.putLong(pos + i, readLong(start() + i));
        for (; i < len; i++)
            bb.put(pos + i, readByte(start() + i));
        return len;
    }

    /**
     * Read a long which is zero padded (high bytes) if the available bytes is less than 8.
     * If the offset is at or beyond the readLimit, this will return 0L.
     *
     * @param offset to read from
     * @return the long which might be padded.
     * @throws IllegalStateException if released
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
     * Returns the actual capacity that can be potentially read.
     *
     * @return the actual capacity that can be potentially read.
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
     * Copy a sub sequence of bytes as a BytesStore.
     *
     * @param start  of bytes
     * @param length of bytes
     * @return ByteStore copy.
     * @throws IllegalStateException if released
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    default BytesStore subBytes(@NonNegative long start, @NonNegative long length)
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.subBytes(this, start, length);
    }

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
     * @throws IllegalStateException if released
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

    default boolean canReadDirect() {
        return canReadDirect();
    }

    default boolean canReadDirect(@NonNegative long length) {
        return isDirectMemory() && readRemaining() >= length;
    }
}
