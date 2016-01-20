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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Created by peter on 12/09/15.
 */
public interface BytesPrepender<B extends BytesPrepender<B>> {

    /**
     * Clear a buffer, with a given padding to allow for prepending later. clearAndPad(0) is the same as clear()
     *
     * @param length to pad
     * @return this
     * @throws BufferOverflowException if the length &gt; capacity() - start()
     */
    B clearAndPad(long length) throws BufferOverflowException;

    /**
     * Prepends a long in decimal, this method moves the readPosition() backwards.
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param value to prepend as text.
     * @return this
     * @throws BufferUnderflowException if the capacity of the underlying buffer was exceeded
     * @throws IORuntimeException       if an error occurred while attempting to resize the underlying buffer
     */
    @NotNull
    default B prepend(long value) throws BufferOverflowException, IORuntimeException {
        BytesInternal.prepend(this, value);
        return (B) this;
    }

    /**
     * Write backward in binary a byte
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param bytes to prepend to.
     */
    B prewrite(byte[] bytes);

    /**
     * Write backward in binary a byte
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param bytes to prepend to.
     */
    B prewrite(BytesStore bytes);

    /**
     * Write backward in binary a byte
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param b byte to prepend to.
     */
    B prewriteByte(byte b);

    /**
     * Write backward in binary a 2 byte int
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param i short to prepend to.
     */
    B prewriteShort(short i);

    /**
     * Write backward in binary a 4 byte int
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param i integer to prepend to.
     */
    B prewriteInt(int i);

    /**
     * Write backward in binary an 8 byte long
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param l long to prepend to.
     */
    B prewriteLong(long l);
}
