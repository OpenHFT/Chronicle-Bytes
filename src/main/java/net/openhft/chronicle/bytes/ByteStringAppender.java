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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Methods to append text to a Bytes. This extends the Appendable interface.
 */
public interface ByteStringAppender<B extends ByteStringAppender<B>> extends StreamingDataOutput<B>, Appendable {

    /**
     * @return these Bytes as a Writer
     */
    default Writer writer() {
        return new ByteStringWriter(this);
    }

    /**
     * Append a char in UTF-8
     *
     * @param ch to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IOException if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(char ch) throws BufferOverflowException, IOException {
        try {
            BytesInternal.appendUTF(this, ch);
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
        return (B) this;
    }

    /**
     * Append a characters in UTF-8
     *
     * @param cs to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IOException if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(@NotNull CharSequence cs) throws BufferOverflowException, IOException {
        return append(cs, 0, cs.length());
    }

    /**
     * Append a long in decimal
     *
     * @param value to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(long value) throws BufferOverflowException, IORuntimeException {
        BytesInternal.append(this, value);
        return (B) this;
    }

    /**
     * Append a float in decimal notation
     *
     * @param f to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(float f) throws BufferOverflowException, IORuntimeException {
        BytesInternal.append((StreamingDataOutput) this, f);
        return (B) this;
    }

    /**
     * Append a double in decimal notation
     *
     * @param d to append
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(double d) throws BufferOverflowException, IORuntimeException {
        BytesInternal.append((StreamingDataOutput) this, d);
        return (B) this;
    }

    /**
     * Append a portion of a String to the Bytes in UTF-8.
     *
     * @param cs    to copy
     * @param start index of the first char inclusive
     * @param end   index of the last char exclusive.
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IOException if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append(@NotNull CharSequence cs, int start, int end)
            throws IndexOutOfBoundsException, BufferOverflowException, IOException {
        try {
            BytesInternal.appendUTF(this, cs, start, end - start);
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
        return (B) this;
    }

    /**
     * Append a String to the Bytes in ISO-8859-1
     *
     * @param cs to write
     * @return this
     * @throws BufferOverflowException  If the string as too large to write in the capacity available
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IORuntimeException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        int length = cs.length();
        for (int i = 0; i < length; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeUnsignedByte(c);
        }
        return (B) this;
    }

    /**
     * Append a portion of a String to the Bytes in ISO-8859-1
     *
     * @param cs    to copy
     * @param start index of the first char inclusive
     * @param end   index of the last char exclusive.
     * @return this
     * @throws BufferOverflowException If the string as too large to write in the capacity available
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException if an error occurred while attempting to resize the underlying buffer
     * @throws IndexOutOfBoundsException if the start or the end are not valid for the CharSequence
     */
    default B append8bit(@NotNull CharSequence cs, int start, int end)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IndexOutOfBoundsException, IORuntimeException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs, (long) start, end);
        }
        for (int i = start; i < end; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeUnsignedByte(c);
        }
        return (B) this;
    }
}
