/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.annotation.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/*
 * Created by Peter Lawrey on 12/09/15.
 */
public interface BytesPrepender<B extends BytesPrepender<B>> {

    /**
     * Clear a buffer, with a given padding to allow for prepending later. clearAndPad(0) is the same as clear()
     *
     * @param length to pad
     * @return this
     * @throws BufferOverflowException if the length &gt; capacity() - start()
     */
    @NotNull
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
    @SuppressWarnings("unchecked")
    @NotNull
    default B prepend(long value) throws BufferOverflowException {
        BytesInternal.prepend(this, value);
        return (B) this;
    }

    /**
     * Write backward in binary a byte
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param bytes to prepend to.
     */
    @NotNull
    B prewrite(byte[] bytes) throws BufferOverflowException;

    /**
     * Write backward in binary a byte
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param bytes to prepend to.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    B prewrite(BytesStore bytes) throws BufferOverflowException;

    /**
     * Write backward in binary a byte
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param b byte to prepend to.
     */
    @NotNull
    B prewriteByte(byte b) throws BufferOverflowException;

    /**
     * Write backward in binary a 2 byte int
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param i short to prepend to.
     */
    @NotNull
    B prewriteShort(short i) throws BufferOverflowException;

    /**
     * Write backward in binary a 4 byte int
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param i integer to prepend to.
     */
    @NotNull
    B prewriteInt(int i) throws BufferOverflowException;

    /**
     * Write backward in binary an 8 byte long
     * <p>Note: it moves the readPosition not the writePosition / readLimit</p>
     *
     * @param l long to prepend to.
     */
    @NotNull
    B prewriteLong(long l) throws BufferOverflowException;
}
