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

import java.nio.ByteOrder;

interface RandomCommon {
    /**
     * @return The smallest position allowed in this buffer.
     */
    default long start() {
        return 0L;
    }

    /**
     * @return the highest limit allowed for this buffer.
     */
    default long capacity() {
        return 1L << 40;
    }

    default long readPosition() {
        return start();
    }

    default long writePosition() {
        return start();
    }

    default long readRemaining() {
        return readLimit() - readPosition();
    }

    default long writeRemaining() {
        return writeLimit() - writePosition();
    }

    /**
     * @return the highest offset or position allowed for this buffer.
     */
    default long readLimit() {
        return capacity();
    }

    default long writeLimit() {
        return capacity();
    }

    /**
     * Obtain the underlying address.  This is for expert users only.
     *
     * @return the underlying address of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     */
    long address() throws UnsupportedOperationException;

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    long accessOffset(long randomOffset);

    /**
     * @return the streaming bytes for reading.
     */
    Bytes bytesForRead();

    /**
     * @return the streaming bytes for writing.
     */
    Bytes bytesForWrite();
}
