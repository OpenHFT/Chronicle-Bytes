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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This data input has a a position() and a limit()
 */
public interface StreamingDataInput<S extends StreamingDataInput<S>> extends StreamingCommon<S> {
    S readPosition(long position);

    S readLimit(long limit);

    /**
     * Perform a set of actions with a temporary bounds mode.
     */
    default void readWithLength(long length, @NotNull Consumer<S> bytesConsumer) {
        parseWithLength(length, (Function<S, Void>) s -> {
            bytesConsumer.accept(s);
            return null;
        });
    }

    default <R> R parseWithLength(long length, @NotNull Function<S, R> bytesConsumer) {
        if (length > readRemaining())
            throw new BufferUnderflowException();
        long limit0 = readLimit();
        long limit = readPosition() + length;
        try {
            readLimit(limit);
            return bytesConsumer.apply((S) this);
        } finally {
            readLimit(limit0);
            readPosition(limit);
        }
    }

    @NotNull
    default InputStream inputStream() {
        return new StreamingInputStream(this);
    }

    default long readStopBit() {
        return BytesInternal.readStopBit(this);
    }

    default double readStopBitDouble() {
        return BytesInternal.readStopBitDouble(this);
    }

    default boolean readBoolean() {
        return readByte() != 0;
    }

    byte readByte();

    int readUnsignedByte();

    short readShort();

    default int readUnsignedShort() {
        return readShort() & 0xFFFF;
    }

    int readInt();

    default long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    long readLong();

    float readFloat();

    double readDouble();

    /**
     * The same as readUTF() except the length is stop bit encoded.  This saves one byte for strings shorter than 128
     * chars.  <code>null</code> values are also supported
     *
     * @return a Unicode string or <code>null</code> if <code>writeUtf8(null)</code> was called
     */
    @Nullable
    default String readUtf8() {
        return BytesInternal.readUtf8(this);
    }

    @Deprecated
    default String readUTFΔ() {
        return BytesInternal.readUtf8(this);
    }

    @Nullable
    default String read8bit() {
        return BytesInternal.read8bit(this);
    }

    /**
     * The same as readUtf8() except the chars are copied to a truncated StringBuilder.
     *
     * @param sb to copy chars to
     * @return <code>true</code> if there was a String, or <code>false</code> if it was <code>null</code>
     */
    default <ACS extends Appendable & CharSequence> boolean readUtf8(@NotNull ACS sb) throws UTFDataFormatRuntimeException {
        AppendableUtil.setLength(sb, 0);
        long len0 = BytesInternal.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        BytesInternal.parseUTF(this, sb, len);
        return true;
    }

    @Deprecated
    default <ACS extends Appendable & CharSequence> boolean readUTFΔ(@NotNull ACS sb) throws UTFDataFormatRuntimeException {
        return readUtf8(sb);
    }

        default boolean read8bit(@NotNull Bytes b) throws UTFDataFormatRuntimeException {
        b.clear();
        long len0 = BytesInternal.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        b.write((BytesStore) this, readPosition(), (long) len);
        readSkip(len);
        return true;
    }

    default <ACS extends Appendable & CharSequence> boolean read8bit(@NotNull ACS sb) throws UTFDataFormatRuntimeException {
        AppendableUtil.setLength(sb, 0);
        long len0 = BytesInternal.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        AppendableUtil.parse8bit(this, sb, len);
        return true;
    }

    default boolean read8bit(StringBuilder sb) {
        sb.setLength(0);
        long len0 = BytesInternal.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        AppendableUtil.parse8bit(this, sb, len);
        return true;
    }

    default void read(@NotNull byte[] bytes) {
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = readByte();
    }

    default int read(@NotNull byte[] bytes, int off, int len) {
        int len2 = (int) Math.min(len, readRemaining());
        for (int i = 0; i < len2; i++)
            bytes[off + i] = readByte();
        return len2;
    }

    default int read(@NotNull char[] bytes, int off, int len) {
        int len2 = (int) Math.min(len, readRemaining());
        for (int i = 0; i < len2; i++)
            bytes[off + i] = (char) readUnsignedByte();
        return len2;
    }

    default void read(@NotNull ByteBuffer buffer) {
        for (int i = (int) Math.min(readRemaining(), buffer.remaining()); i > 0; i--)
            buffer.put(readByte());
    }

    int readVolatileInt();

    long readVolatileLong();

    int peekUnsignedByte();

    /**
     * This is an expert level method for copying raw native memory in bulk.
     *
     * @param address of the memory.
     * @param size    in bytes.
     */
    void nativeRead(long address, long size);

    default <E extends Enum<E>> E readEnum(Class<E> eClass) {
        return BytesInternal.readEnum(this, eClass);
    }

    default void parseUTF(Appendable sb, int length) {
        AppendableUtil.setLength(sb, 0);
        BytesInternal.parseUTF(this, sb, length);
    }
}
