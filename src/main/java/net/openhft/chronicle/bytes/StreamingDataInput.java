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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
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

/**
 * This data input has a a position() and a limit()
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface StreamingDataInput<S extends StreamingDataInput<S>> extends StreamingCommon<S> {
    @NotNull
    S readPosition(long position) throws BufferUnderflowException;

    @NotNull
    default S readPositionUnlimited(long position) throws BufferUnderflowException {
        return readLimitToCapacity().readPosition(position);
    }

    @NotNull
    default S readPositionRemaining(long position, long remaining) throws BufferUnderflowException {
        readLimit(position + remaining);
        return readPosition(position);
    }

    @NotNull
    S readLimit(long limit) throws BufferUnderflowException;

    default S readLimitToCapacity() throws BufferUnderflowException {
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
    S readSkip(long bytesToSkip) throws BufferUnderflowException;

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
            throws BufferUnderflowException, IORuntimeException {
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
            throws BufferUnderflowException, IORuntimeException {
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

    default long readStopBit() throws IORuntimeException {
        return BytesInternal.readStopBit(this);
    }

    default char readStopBitChar() throws IORuntimeException {
        return BytesInternal.readStopBitChar(this);
    }

    default double readStopBitDouble() {
        return BytesInternal.readStopBitDouble(this);
    }

    default double readStopBitDecimal() throws BufferOverflowException {
        long value = readStopBit();
        int scale = (int) (Math.abs(value) % 10);
        value /= 10;
        return (double) value / Maths.tens(scale);
    }

    default boolean readBoolean() {
        byte b = readByte();
        return BytesUtil.byteToBoolean(b);
    }

    byte readByte();

    default byte rawReadByte() {
        return readByte();
    }

    default char readChar() {
        return readStopBitChar();
    }

    /**
     * @return the next unsigned 8 bit value or -1;
     */
    int readUnsignedByte();

    /**
     * @return the next unsigned 8 bit value or -1;
     */
    int uncheckedReadUnsignedByte();

    short readShort() throws BufferUnderflowException;

    default int readUnsignedShort() throws BufferUnderflowException {
        return readShort() & 0xFFFF;
    }

    default int readInt24() throws BufferUnderflowException {
        return readUnsignedShort() | (readUnsignedByte() << 24 >> 8);
    }

    default int readUnsignedInt24() throws BufferUnderflowException {
        return readUnsignedShort() | (readUnsignedByte() << 16);
    }

    int readInt() throws BufferUnderflowException;

    default int rawReadInt() {
        return readInt();
    }

    default long readUnsignedInt()
            throws BufferUnderflowException {
        return readInt() & 0xFFFFFFFFL;
    }

    long readLong() throws BufferUnderflowException;

    default long rawReadLong() {
        return readLong();
    }

    /**
     * @return a long using the bytes remaining
     */
    default long readIncompleteLong() {
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

    float readFloat() throws BufferUnderflowException;

    double readDouble() throws BufferUnderflowException;

    /**
     * The same as readUTF() except the length is stop bit encoded.  This saves one byte for strings shorter than 128
     * chars.  <code>null</code> values are also supported
     *
     * @return a Unicode string or <code>null</code> if <code>writeUtf8(null)</code> was called
     */
    @Nullable
    default String readUtf8()
            throws BufferUnderflowException, IORuntimeException, IllegalArgumentException {
        return BytesInternal.readUtf8(this);
    }

    @Nullable
    @Deprecated(/* to be removed in x.22 */)
    default String readUTFΔ()
            throws IORuntimeException, BufferUnderflowException, IllegalArgumentException {
        return BytesInternal.readUtf8(this);
    }

    @Nullable
    default String read8bit() throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.read8bit(this);
    }

    /**
     * The same as readUtf8() except the chars are copied to a truncated StringBuilder.
     *
     * @param sb to copy chars to
     * @return <code>true</code> if there was a String, or <code>false</code> if it was <code>null</code>
     */
    default <ACS extends Appendable & CharSequence> boolean readUtf8(@NotNull ACS sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        AppendableUtil.setLength(sb, 0);
        if (readRemaining() <= 0)
            // TODO throw BufferUnderflowException here? please review
            return false;
        long len0 = readStopBit();
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        if (len > 0)
            BytesInternal.parseUtf8(this, sb, true, len);
        return true;
    }

    @Deprecated(/* to be removed in x.22 */)
    default <ACS extends Appendable & CharSequence> boolean readUTFΔ(@NotNull ACS sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        return readUtf8(sb);
    }

    default boolean read8bit(@NotNull Bytes b)
            throws BufferUnderflowException, IllegalStateException, BufferOverflowException {
        b.clear();
        if (readRemaining() <= 0)
            return false;
        long len0 = this.readStopBit();
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        b.write((BytesStore) this, readPosition(), len);
        readSkip(len);
        return true;
    }

    default <ACS extends Appendable & CharSequence> boolean read8bit(@NotNull ACS sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        AppendableUtil.setLength(sb, 0);
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
            throws IORuntimeException, BufferUnderflowException {
        sb.setLength(0);
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

    default int read(@NotNull byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    default int read(@NotNull byte[] bytes, int off, int len) {
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

    default int read(@NotNull char[] bytes, int off, int len) {
        long remaining = readRemaining();
        if (remaining <= 0)
            return -1;
        int len2 = (int) Math.min(len, remaining);
        for (int i = 0; i < len2; i++)
            bytes[off + i] = (char) readUnsignedByte();
        return len2;
    }

    default void read(@NotNull ByteBuffer buffer) {
        for (int i = (int) Math.min(readRemaining(), buffer.remaining()); i > 0; i--)
            buffer.put(readByte());
    }

    default void read(@NotNull Bytes bytes, int length) {
        int len2 = (int) Math.min(length, readRemaining());
        int i = 0;
        for (; i < len2 - 7; i += 8)
            bytes.rawWriteLong(rawReadLong());
        for (; i < len2; i++)
            bytes.rawWriteByte(rawReadByte());
    }

    default void unsafeReadObject(Object o, int length) {
        unsafeReadObject(o, (o.getClass().isArray() ? 4 : 0) + (OS.is64Bit() ? 12 : 8), length);
    }

    default void unsafeReadObject(Object o, int offset, int length) {
        assert BytesUtil.isTriviallyCopyable(o.getClass(), offset, length);
        if (readRemaining() < length)
            throw new BufferUnderflowException();
        int i = 0;
        for (; i < length - 7; i += 8)
            UnsafeMemory.unsafePutLong(o, offset + i, rawReadLong());
        for (; i < length; i++)
            UnsafeMemory.unsafePutByte(o, offset + i, rawReadByte());
    }

    int readVolatileInt() throws BufferUnderflowException;

    long readVolatileLong() throws BufferUnderflowException;

    int peekUnsignedByte();


    @NotNull
    default <E extends Enum<E>> E readEnum(@NotNull Class<E> eClass)
            throws IORuntimeException, BufferUnderflowException {
        return BytesInternal.readEnum(this, eClass);
    }

    @Deprecated(/* to be removed in x.22 */)
    default void parseUTF(Appendable sb, int length)
            throws IllegalArgumentException, BufferUnderflowException, UTFDataFormatRuntimeException {
        parseUtf8(sb, length);
    }

    default void parseUtf8(Appendable sb, int encodedLength)
            throws IllegalArgumentException, BufferUnderflowException, UTFDataFormatRuntimeException {
        parseUtf8(sb, true, encodedLength);
    }

    default void parseUtf8(Appendable sb, boolean utf, int length)
            throws IllegalArgumentException, BufferUnderflowException, UTFDataFormatRuntimeException {
        AppendableUtil.setLength(sb, 0);
        BytesInternal.parseUtf8(this, sb, utf, length);
    }

    default long parseHexLong() {
        return BytesInternal.parseHexLong(this);
    }

    void copyTo(OutputStream out) throws IOException;

    long copyTo(BytesStore to);

    default void readHistogram(@NotNull Histogram histogram) {
        BytesInternal.readHistogram(this, histogram);
    }

    default void readWithLength(Bytes bytes) {
        bytes.clear();
        int length = Maths.toUInt31(readStopBit());
        int i;
        for (i = 0; i < length - 7; i++)
            bytes.writeLong(readLong());
        for (; i < length; i++)
            bytes.writeByte(readByte());
    }

    /**
     * When there is no more data to read, return zero, false and empty string.
     *
     * @param lenient if true, return nothing rather than error.
     */
    void lenient(boolean lenient);

    boolean lenient();

}
