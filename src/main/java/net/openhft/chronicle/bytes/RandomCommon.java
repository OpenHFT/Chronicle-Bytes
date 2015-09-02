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

import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.annotation.ForceInline;

import java.nio.BufferOverflowException;
import java.nio.ByteOrder;

interface RandomCommon extends ReferenceCounted {
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

    /**
     * The read position must be start() &lt;= readPosition() &amp;&amp; readPosition() &lt;= readLimit() &amp;&amp; readPosition &lt; safeLimit()
     *
     * @return position to read from.
     */
    @ForceInline
    default long readPosition() {
        return start();
    }

    /**
     * The read position must be readPosition() &lt;= writePosition() &amp;&amp; writePosition() &lt;= writeLimit()
     * @return position to write to.
     */
    @ForceInline
    default long writePosition() {
        return start();
    }

    /**
     * @return How many more bytes can we read.
     */
    @ForceInline
    default long readRemaining() {
        return readLimit() - readPosition();
    }

    /**
     * @return How many more bytes can we written.
     */
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
     * @throws IllegalArgumentException if the offset is before the start() or the after the capacity()
     */
    long address(long offset)
            throws UnsupportedOperationException, IllegalArgumentException;

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * @return the streaming bytes for reading.
     */
    Bytes bytesForRead() throws IllegalStateException;

    /**
     * @return the streaming bytes for writing.
     */
    Bytes bytesForWrite() throws IllegalStateException;

    /**
     * Perform a 32-bit CAS at a given offset.
     * @param offset to perform CAS
     * @param expected value
     * @param value to set
     * @return true, if successful.
     */
    boolean compareAndSwapInt(long offset, int expected, int value)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    /**
     * Perform a 64-bit CAS at a given offset.
     * @param offset to perform CAS
     * @param expected value
     * @param value to set
     * @return true, if successful.
     */
    boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException;

    /**
     * Perform a 32-bit float CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    default boolean compareAndSwapFloat(long offset, float expected, float value)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return compareAndSwapInt(offset, Float.floatToRawIntBits(expected), Float.floatToRawIntBits(value));
    }

    /**
     * Perform a 64-bit double CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    default boolean compareAndSwapDouble(long offset, double expected, double value)
            throws BufferOverflowException, IllegalArgumentException, IORuntimeException {
        return compareAndSwapLong(offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
    }
}
