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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

/**
 * This allows random access to the underling bytes.  This instance can be used across threads as it is stateless.
 * The thread safety of the underlying data depends on how the methods are used.
 */
public interface RandomDataInput extends RandomCommon {
    String[] charToString = createCharToString();

    @NotNull
    static String[] createCharToString() {
        String[] charToString = new String[256];
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
     * Read boolean at an offset
     *
     * @param offset to read
     * @return the boolean
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    @ForceInline
    default boolean readBoolean(long offset)
            throws BufferUnderflowException, IORuntimeException {
        return readByte(offset) != 0;
    }

    /**
     * Read byte at an offset
     *
     * @param offset to read
     * @return the byte
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    byte readByte(long offset) throws BufferUnderflowException, IORuntimeException;

    /**
     * Read an unsigned byte at an offset
     *
     * @param offset to read
     * @return the unsigned byte
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    @ForceInline
    default int readUnsignedByte(long offset)
            throws BufferUnderflowException, IORuntimeException {
        return readByte(offset) & 0xFF;
    }

    /**
     * Read a short at an offset
     *
     * @param offset to read
     * @return the short
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    short readShort(long offset) throws BufferUnderflowException, IORuntimeException;

    /**
     * Read an unsigned short at an offset
     *
     * @param offset to read
     * @return the unsigned short
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    @ForceInline
    default int readUnsignedShort(long offset)
            throws BufferUnderflowException, IORuntimeException {
        return readShort(offset) & 0xFFFF;
    }

    /**
     * Read an int at an offset
     *
     * @param offset to read
     * @return the int
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    int readInt(long offset) throws BufferUnderflowException, IORuntimeException;

    /**
     * Read an unsigned int at an offset
     *
     * @param offset to read
     * @return the unsigned int
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    @ForceInline
    default long readUnsignedInt(long offset)
            throws BufferUnderflowException, IORuntimeException {
        return readInt(offset) & 0xFFFFFFFFL;
    }

    /**
     * Read a long at an offset
     *
     * @param offset to read
     * @return the long
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    long readLong(long offset) throws BufferUnderflowException, IORuntimeException;

    /**
     * Read a float at an offset
     *
     * @param offset to read
     * @return the float
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    float readFloat(long offset) throws BufferUnderflowException, IORuntimeException;

    /**
     * Read a double at an offset
     *
     * @param offset to read
     * @return the double
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    double readDouble(long offset) throws BufferUnderflowException, IORuntimeException;

    /**
     * Read the byte at an offset and converts it into a printable
     *
     * @param offset to read
     * @return the byte in a printable form.
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default String printable(long offset) throws BufferUnderflowException, IORuntimeException {
        return charToString[readUnsignedByte(offset)];
    }

    /**
     * Read a 32-bit int from memory with a load barrier.
     *
     * @param offset to read
     * @return the int value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default int readVolatileInt(long offset)
            throws BufferUnderflowException, IORuntimeException {
        OS.memory().loadFence();
        return readInt(offset);
    }

    /**
     * Read a float from memory with a load barrier.
     *
     * @param offset to read
     * @return the float value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default float readVolatileFloat(long offset)
            throws BufferUnderflowException, IORuntimeException {
        return Float.intBitsToFloat(readVolatileInt(offset));
    }

    /**
     * Read a 64-bit long from memory with a load barrier.
     *
     * @param offset to read
     * @return the long value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default long readVolatileLong(long offset)
            throws BufferUnderflowException, IORuntimeException {
        OS.memory().loadFence();
        return readLong(offset);
    }

    /**
     * Read a 64-bit double from memory with a load barrier.
     *
     * @param offset to read
     * @return the double value
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default double readVolatileDouble(long offset)
            throws BufferUnderflowException, IORuntimeException {
        return Double.longBitsToDouble(readVolatileLong(offset));
    }

    default long parseLong(long offset) throws BufferUnderflowException, IORuntimeException {
        return BytesInternal.parseLong(this, offset);
    }

    /**
     * expert level method for copying data to native memory.
     *
     * @param position within the ByteStore to copy.
     * @param address  in native memory
     * @param size     in bytes
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    void nativeRead(long position, long address, long size);

    /**
     * Read a byte[] from memory.
     *
     * @return the length actually read.
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default int copyTo(@NotNull byte[] bytes)
            throws BufferUnderflowException, IORuntimeException {
        int len = (int) Math.min(bytes.length, readRemaining());
        for (int i = 0; i < len; i++)
            bytes[i] = readByte(start() + i);
        return len;
    }

    /**
     * Read a long which is zero padded (high bytes) if the available bytes is less than 8.
     * If the offset is at or beyond the readLimit, this will return 0L.
     *
     * @param offset to read from
     * @return the long which might be padded.
     * @throws IORuntimeException if an error occurred trying to obtain the data.
     */
    default long readIncompleteLong(long offset) throws IORuntimeException {
        long left = readRemaining() - offset;
        long l = 0;
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
     * @return the actual capacity you can potentially read.
     */
    long realCapacity();

    /**
     * Perform an atomic add and get operation for a 32-bit int
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default int addAndGetInt(long offset, int adding)
            throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.addAndGetInt(this, offset, adding);
    }

    /**
     * Perform an atomic add and get operation for a 64-bit long
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default long addAndGetLong(long offset, long adding)
            throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.addAndGetLong(this, offset, adding);
    }

    /**
     * Perform an atomic add and get operation for a 32-bit float
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default float addAndGetFloat(long offset, float adding)
            throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.addAndGetFloat(this, offset, adding);
    }

    /**
     * Perform an atomic add and get operation for a 64-bit double
     *
     * @param offset to add and get
     * @param adding value to add, can be 1
     * @return the sum
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     * @throws IORuntimeException       if an error occurred trying to obtain the data.
     */
    default double addAndGetDouble(long offset, double adding)
            throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.addAndGetDouble(this, offset, adding);
    }

    /**
     * Copy a sub sequence of bytes as a BytesStore.
     *
     * @param start  of bytes
     * @param length of bytes
     * @return ByteStore copy.
     */
    default BytesStore subBytes(long start, long length) {
        return BytesInternal.subBytes(this, start, length);
    }

    default int findByte(byte stopByte) {
        return BytesInternal.findByte(this, stopByte);
    }

    /**
     * Truncates {@code sb} (it must be a {@link StringBuilder} or {@link Bytes}) and reads a char
     * sequence from the given {@code offset}, encoded as Utf8, into it. Returns offset <i>after</i>
     * the read Utf8, if a normal char sequence was read, or {@code -1 - offset}, if {@code null}
     * was observed (in this case, {@code sb} is truncated too, but not updated then, by querying
     * {@code sb} only this case is indistinguishable from reading an empty char sequence).
     *
     * @param offset the offset in this {@code RandomDataInput} to read char sequence from
     * @param sb the buffer to read char sequence into (truncated first)
     * @param <ACS> buffer type, must be {@code StringBuilder} or {@code Bytes}
     * @return offset after the normal read char sequence, or -1 - offset, if char sequence is
     * {@code null}
     * @see RandomDataOutput#writeUtf8(long, CharSequence)
     */
    default <ACS extends Appendable & CharSequence> long readUtf8(long offset, @NotNull ACS sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        AppendableUtil.setLength(sb, 0);
        // TODO insert some bounds check here

        long utflen;
        if ((utflen = readByte(offset++)) < 0) {
            utflen &= 0x7FL;
            long b;
            int count = 7;
            while ((b = readByte(offset++)) < 0) {
                utflen |= (b & 0x7FL) << count;
                count += 7;
            }
            if (b != 0) {
                if (count > 56)
                    throw new IORuntimeException(
                            "Cannot read more than 9 stop bits of positive value");
                utflen |= (b << count);
            } else {
                if (count > 63)
                    throw new IORuntimeException(
                            "Cannot read more than 10 stop bits of negative value");
                utflen = ~utflen;
            }
        }
        if (utflen == -1)
            return ~offset;
        int len = Maths.toUInt31(utflen);
        BytesInternal.parseUTF(this, offset, sb, len);
        return offset + utflen;
    }
}
