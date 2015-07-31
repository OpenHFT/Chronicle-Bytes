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
        throw new UnsupportedOperationException();
    }

    default long readStopBit() {
        return BytesUtil.readStopBit(this);
    }

    default boolean readBoolean() {
        return readByte() != 0;
    }

    byte readByte();

    default int readUnsignedByte() {
        return readByte() & 0xFF;
    }

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
     * @return a Unicode string or <code>null</code> if <code>writeUTFΔ(null)</code> was called
     */
    @Nullable
    default String readUTFΔ() {
        return BytesUtil.readUTFΔ(this);
    }

    /**
     * The same as readUTFΔ() except the chars are copied to a truncated StringBuilder.
     *
     * @param sb to copy chars to
     * @return <code>true</code> if there was a String, or <code>false</code> if it was <code>null</code>
     */
    default <ACS extends Appendable & CharSequence> boolean readUTFΔ(@NotNull ACS sb) throws UTFDataFormatRuntimeException {
        BytesUtil.setLength(sb, 0);
        long len0 = BytesUtil.readStopBit(this);
        if (len0 == -1)
            return false;
        int len = Maths.toUInt31(len0);
        BytesUtil.parseUTF(this, sb, len);
        return true;
    }

    default void read(@NotNull byte[] bytes) {
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = readByte();
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
}
