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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.core.util.ThrowingConsumer;
import net.openhft.chronicle.core.util.ThrowingConsumerNonCapturing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;

/**
 * This data input has a a position() and a limit()
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface StreamingDataInput<S extends StreamingDataInput<S>> extends StreamingCommon<S> {
    @NotNull
    S readPosition(long position)
            throws BufferUnderflowException, IllegalStateException;

    @NotNull
    default S readPositionUnlimited(long position)
            throws BufferUnderflowException, IllegalStateException {
        return readLimitToCapacity().readPosition(position);
    }

    @NotNull
    default S readPositionRemaining(long position, long remaining)
            throws BufferUnderflowException, IllegalStateException {
        readLimit(position + remaining);
        return readPosition(position);
    }

    @NotNull
    S readLimit(long limit)
            throws BufferUnderflowException;

    default S readLimitToCapacity()
            throws BufferUnderflowException {
        return readLimit(capacity());
    }

    /**
     * Skip a number of bytes by moving the readPosition. Must be less than or equal to the readLimit.
     *
     * @param bytesToSkip bytes to skip.
     * @return this
     * @throws BufferUnderflowException if the offset is outside the limits of the Bytes
     */
    @NotNull
    S readSkip(long bytesToSkip)
            throws BufferUnderflowException, IllegalStateException;

    /**
     * obtain the readPosition skipping any padding needed for a header.
     *
     * @param skipPadding optional aligning to 4 bytes
     * @return the read position.
     */
    default long readPositionForHeader(boolean skipPadding) {
        long position = readPosition();
        if (skipPadding)
            return readSkip(BytesUtil.padOffset(position)).readPosition();
        return position;
    }

    /**
     * Read skip 1 when you are sure this is safe. Use at your own risk when you find a performance problem.
     */
    void uncheckedReadSkipOne();

    /**
     * Read skip -1 when you are sure this is safe. Use at your own risk when you find a performance problem.
     */
    void uncheckedReadSkipBackOne();

    /**
     * Perform a set of actions with a temporary bounds mode.
     */
    default void readWithLength0(long length, @NotNull ThrowingConsumerNonCapturing<S, IORuntimeException, BytesOut> bytesConsumer, StringBuilder sb, BytesOut toBytes)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        if (length > readRemaining())
            throw new BufferUnderflowException();
        long limit0 = readLimit();
        long limit = readPosition() + length;
        try {
            readLimit(limit);
            bytesConsumer.accept((S) this, sb, toBytes);
        } finally {
            readLimit(limit0);
            readPosition(limit);
        }
    }

    /**
     * Perform a set of actions with a temporary bounds mode.
     */
    default void readWithLength(long length, @NotNull ThrowingConsumer<S, IORuntimeException> bytesConsumer)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        if (length > readRemaining())
            throw new BufferUnderflowException();
        long limit0 = readLimit();
        long limit = readPosition() + length;
        try {
            readLimit(limit);
            bytesConsumer.accept((S) this);
        } finally {
            readLimit(limit0);
            readPosition(limit);
        }
    }

    @NotNull
    default InputStream inputStream() {
        return new StreamingInputStream(this);
    }

    default long readStopBit()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        return BytesInternal.readStopBit(this);
    }

    default char readStopBitChar()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        return BytesInternal.readStopBitChar(this);
    }

    default double readStopBitDouble()
            throws IllegalStateException {
        return BytesInternal.readStopBitDouble(this);
    }

    default double readStopBitDecimal()
            throws IllegalStateException, BufferUnderflowException {
        long value = readStopBit();
        int scale = (int) (Math.abs(value) % 10);
        value /= 10;
        return (double) value / Maths.tens(scale);
    }

    default boolean readBoolean()
            throws IllegalStateException {
        byte b = readByte();
        return BytesUtil.byteToBoolean(b);
    }

    byte readByte()
            throws IllegalStateException;

    default byte rawReadByte()
            throws IllegalStateException {
        return readByte();
    }

    default char readChar()
            throws IllegalStateException, BufferUnderflowException {
        return readStopBitChar();
    }

    /**
     * @return the next unsigned 8 bit value or -1;
     */
    int readUnsignedByte()
            throws IllegalStateException;

    /**
     * @return the next unsigned 8 bit value or -1;
     */
    int uncheckedReadUnsignedByte();

    short readShort()
            throws BufferUnderflowException, IllegalStateException;

    default int readUnsignedShort()
            throws BufferUnderflowException, IllegalStateException {
        return readShort() & 0xFFFF;
    }

    default int readInt24()
            throws BufferUnderflowException, IllegalStateException {
        return readUnsignedShort() | (readUnsignedByte() << 24 >> 8);
    }

    default int readUnsignedInt24()
            throws BufferUnderflowException, IllegalStateException {
        return readUnsignedShort() | (readUnsignedByte() << 16);
    }

    int readInt()
            throws BufferUnderflowException, IllegalStateException;

    default int rawReadInt()
            throws BufferUnderflowException, IllegalStateException {
        return readInt();
    }

    default long readUnsignedInt()
            throws BufferUnderflowException, IllegalStateException {
        return readInt() & 0xFFFFFFFFL;
    }

    long readLong()
            throws BufferUnderflowException, IllegalStateException;

    default long rawReadLong()
            throws BufferUnderflowException, IllegalStateException {
        return readLong();
    }

    /**
     * @return a long using the bytes remaining
     */
    default long readIncompleteLong()
            throws IllegalStateException {
        long left = readRemaining();
        try {
            if (left >= 8)
                return readLong();
            if (left == 4)
                return readInt();
            long l = 0;
            for (int i = 0, remaining = (int) left; i < remaining; i++) {
                l |= (long) readUnsignedByte() << (i * 8);
            }
            return l;

        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    float readFloat()
            throws BufferUnderflowException, IllegalStateException;

    double readDouble()
            throws BufferUnderflowException, IllegalStateException;

    /**
     * The same as readUTF() except the length is stop bit encoded.  This saves one byte for strings shorter than 128
     * chars.  <code>null</code> values are also supported
     *
     * @return a Unicode string or <code>null</code> if <code>writeUtf8(null)</code> was called
     */
    @Nullable
    default String readUtf8()
            throws BufferUnderflowException, IORuntimeException, IllegalStateException, ArithmeticException {
        return BytesInternal.readUtf8(this);
    }

    @Nullable
    default String read8bit()
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.read8bit(this);
    }

    /**
     * The same as readUtf8() except the chars are copied to a truncated StringBuilder.
     *
     * @param sb to copy chars to
     * @return <code>true</code> if there was a String, or <code>false</code> if it was <code>null</code>
     */
    default <ACS extends Appendable & CharSequence> boolean readUtf8(@NotNull ACS sb)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalStateException, IllegalArgumentException {
        try {
            AppendableUtil.setLength(sb, 0);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        if (readRemaining() <= 0)
            return true;
        long len0 = readStopBit();
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        if (len > 0)
            BytesInternal.parseUtf8(this, sb, true, len);
        return true;
    }

    default boolean readUtf8(@NotNull Bytes sb)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalStateException {
        sb.readPositionRemaining(0, 0);
        if (readRemaining() <= 0)
            return true;
        long len0 = readStopBit();
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        if (len > 0)
            BytesInternal.parseUtf8(this, sb, true, len);
        return true;
    }

    default boolean readUtf8(@NotNull StringBuilder sb)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalStateException {
        sb.setLength(0);
        if (readRemaining() <= 0)
            return true;
        long len0 = readStopBit();
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        if (len > 0)
            BytesInternal.parseUtf8(this, sb, true, len);
        return true;
    }

    default boolean read8bit(@NotNull Bytes b)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException, BufferOverflowException {
        b.clear();
        if (readRemaining() <= 0)
            return true;
        long len0;
        byte b1;
        if ((b1 = rawReadByte()) >= 0) {
            len0 = b1;
        } else if (b1 == -128 && peekUnsignedByte() == 0) {
            ((StreamingDataInput) this).readSkip(1);
            return false;
        } else {
            len0 = BytesInternal.readStopBit0(this, b1);
        }
        try {
            int len = Maths.toUInt31(len0);
            b.write((BytesStore) this, readPosition(), len);
            readSkip(len);
            return true;
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Deprecated(/* remove in x.23 */)
    default <ACS extends Appendable & CharSequence> boolean read8bit(@NotNull ACS sb)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalArgumentException, IllegalStateException {
        AppendableUtil.setLength(sb, 0);
        if (readRemaining() <= 0)
            return true;
        long len0 = BytesInternal.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        try {
            AppendableUtil.parse8bit(this, sb, len);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return true;
    }

    default boolean read8bit(@NotNull StringBuilder sb)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalStateException {
        sb.setLength(0);
        if (readRemaining() <= 0)
            return true;
        long len0 = BytesInternal.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        try {
            AppendableUtil.parse8bit(this, sb, len);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return true;
    }

    default int read(@NotNull byte[] bytes)
            throws BufferUnderflowException, IllegalStateException {
        return read(bytes, 0, bytes.length);
    }

    default int read(@NotNull byte[] bytes, int off, int len)
            throws BufferUnderflowException, IllegalStateException {
        long remaining = readRemaining();
        if (remaining <= 0)
            return -1;
        int len2 = (int) Math.min(len, remaining);
        int i = 0;
        for (; i < len2 - 7; i += 8)
            UnsafeMemory.unsafePutLong(bytes, i + off, rawReadLong());
        for (; i < len2; i++)
            bytes[off + i] = rawReadByte();
        return len2;
    }

    default int read(@NotNull char[] bytes, int off, int len)
            throws IllegalStateException {
        long remaining = readRemaining();
        if (remaining <= 0)
            return -1;
        int len2 = (int) Math.min(len, remaining);
        for (int i = 0; i < len2; i++)
            bytes[off + i] = (char) readUnsignedByte();
        return len2;
    }

    default void read(@NotNull ByteBuffer buffer)
            throws IllegalStateException {
        for (int i = (int) Math.min(readRemaining(), buffer.remaining()); i > 0; i--)
            buffer.put(readByte());
    }

    /**
     * Transfer as many bytes as possible.
     * If you want to write all the bytes or fail use write.
     *
     * @param bytes to copy to.
     * @see StreamingDataOutput#write(BytesStore)
     */
    default void read(Bytes bytes) {
        int length = Math.toIntExact(Math.min(readRemaining(), bytes.writeRemaining()));
        read(bytes, length);
    }

    default void read(@NotNull Bytes bytes, int length)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        int len2 = (int) Math.min(length, readRemaining());
        int i = 0;
        for (; i < len2 - 7; i += 8)
            bytes.rawWriteLong(rawReadLong());
        for (; i < len2; i++)
            bytes.rawWriteByte(rawReadByte());
    }

    default void unsafeReadObject(Object o, int length)
            throws BufferUnderflowException, IllegalStateException {
        unsafeReadObject(o, (o.getClass().isArray() ? 4 : 0) + Jvm.objectHeaderSize(), length);
    }

    default void unsafeReadObject(Object o, int offset, int length)
            throws BufferUnderflowException, IllegalStateException {
        assert BytesUtil.isTriviallyCopyable(o.getClass(), offset, length);
        if (readRemaining() < length)
            throw new BufferUnderflowException();
        if (isDirectMemory()) {
            MEMORY.copyMemory(addressForRead(readPosition()), o, offset, length);
            readSkip(length);
            return;
        }
        int i = 0;
        for (; i < length - 7; i += 8)
            UnsafeMemory.unsafePutLong(o, (long) offset + i, rawReadLong());
        if (i < length - 3) {
            UnsafeMemory.unsafePutInt(o, (long) offset + i, rawReadInt());
            i += 4;
        }
        for (; i < length; i++)
            UnsafeMemory.unsafePutByte(o, (long) offset + i, rawReadByte());
    }

    default S unsafeRead(long address, int length) {
        if (isDirectMemory()) {
            long src = addressForRead(readPosition());
            readSkip(length);
            MEMORY.copyMemory(src, address, length);
        } else {
            int i = 0;
            for (; i < length - 7; i += 8)
                MEMORY.writeLong(address + i, readLong());
            for (; i < length; ++i)
                MEMORY.writeByte(address + i, readByte());
        }

        return (S) this;
    }

    int readVolatileInt()
            throws BufferUnderflowException, IllegalStateException;

    long readVolatileLong()
            throws BufferUnderflowException, IllegalStateException;

    int peekUnsignedByte()
            throws IllegalStateException;

    @NotNull
    default <E extends Enum<E>> E readEnum(@NotNull Class<E> eClass)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalStateException, BufferOverflowException {
        return BytesInternal.readEnum(this, eClass);
    }

    /**
     * parse a UTF8 string.
     *
     * @param sb            buffer to copy into
     * @param encodedLength length of the UTF encoded data in bytes
     */
    default void parseUtf8(Appendable sb, int encodedLength)
            throws IllegalArgumentException, BufferUnderflowException, UTFDataFormatRuntimeException, IllegalStateException {
        parseUtf8(sb, true, encodedLength);
    }

    /**
     * parse a UTF8 string.
     *
     * @param sb     buffer to copy into
     * @param utf    true if the length is the UTF-8 encoded length, false if the length is the length of chars
     * @param length to limit the read.
     */
    default void parseUtf8(Appendable sb, boolean utf, int length)
            throws IllegalArgumentException, BufferUnderflowException, UTFDataFormatRuntimeException, IllegalStateException {
        AppendableUtil.setLength(sb, 0);
        BytesInternal.parseUtf8(this, sb, utf, length);
    }

    default long parseHexLong()
            throws BufferUnderflowException, IllegalStateException {
        return BytesInternal.parseHexLong(this);
    }

    void copyTo(OutputStream out)
            throws IOException, IllegalStateException;

    long copyTo(BytesStore to)
            throws IllegalStateException;

    default void readHistogram(@NotNull Histogram histogram)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        BytesInternal.readHistogram(this, histogram);
    }

    default void readWithLength(Bytes bytes)
            throws ArithmeticException, BufferUnderflowException, BufferOverflowException, IllegalStateException {
        bytes.clear();
        int length = Maths.toUInt31(readStopBit());
        int i;
        for (i = 0; i < length - 7; i++)
            bytes.writeLong(readLong());
        for (; i < length; i++)
            bytes.writeByte(readByte());
    }

    /**
     * When there is no more data to read, return zero, <code>false</code> and empty string.
     *
     * @param lenient if true, return nothing rather than error.
     */
    void lenient(boolean lenient);

    boolean lenient();

}
