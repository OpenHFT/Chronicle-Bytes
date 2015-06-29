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

import net.openhft.chronicle.core.annotation.ForceInline;

import java.nio.ByteOrder;

interface RandomCommon {
    /**
     * @return The smallest position allowed in this buffer.
     */
    @ForceInline
    default long start() {
        return 0L;
    }

    /**
     * @return the highest limit allowed for this buffer.
     */
    @ForceInline
    default long capacity() {
        return Bytes.MAX_CAPACITY;
    }

    @ForceInline
    default long readPosition() {
        return start();
    }

    @ForceInline
    default long writePosition() {
        return start();
    }

    @ForceInline
    default long readRemaining() {
        return readLimit() - readPosition();
    }

    @ForceInline
    default long writeRemaining() {
        return writeLimit() - writePosition();
    }

    /**
     * @return the highest offset or position allowed for this buffer.
     */
    @ForceInline
    default long readLimit() {
        return capacity();
    }

    @ForceInline
    default long writeLimit() {
        return capacity();
    }

    /**
     * Obtain the underlying address.  This is for expert users only.
     *
     * @param offset within this buffer. address(start()) is the actual address of the first byte.
     * @return the underlying address of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     */
    long address(long offset) throws UnsupportedOperationException;

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * @return the streaming bytes for reading.
     */
    Bytes bytesForRead();

    /**
     * @return the streaming bytes for writing.
     */
    Bytes bytesForWrite();
}
