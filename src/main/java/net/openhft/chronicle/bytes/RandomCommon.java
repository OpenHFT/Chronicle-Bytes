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

import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
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
     *
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
        long remaining = readLimit() - readPosition();
        return remaining;
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
     * Obtain the underlying addressForRead.  This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     * @throws BufferUnderflowException      if the offset is before the start() or the after the capacity()
     */
    long addressForRead(long offset)
            throws UnsupportedOperationException, BufferUnderflowException;

    default long addressForRead(long offset, int buffer)
            throws UnsupportedOperationException, BufferUnderflowException {
        return addressForRead(offset);
    }

    /**
     * Obtain the underlying addressForRead.  This is for expert users only.
     *
     * @param offset within this buffer. addressForRead(start()) is the actual addressForRead of the first byte.
     * @return the underlying addressForRead of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     * @throws BufferOverflowException       if the offset is before the start() or the after the capacity()
     */
    long addressForWrite(long offset)
            throws UnsupportedOperationException, BufferOverflowException;

    long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException;

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    /**
     * @return the streaming bytes for reading.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes bytesForRead() throws IllegalStateException;

    /**
     * @return the streaming bytes for writing.
     */
    @SuppressWarnings("rawtypes")
    @NotNull
    Bytes bytesForWrite() throws IllegalStateException;

    /**
     * Perform a 32-bit CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    boolean compareAndSwapInt(long offset, int expected, int value)
            throws BufferOverflowException;

    void testAndSetInt(long offset, int expected, int value);

    /**
     * Perform a 64-bit CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException;

    /**
     * Perform a 32-bit float CAS at a given offset.
     *
     * @param offset   to perform CAS
     * @param expected value
     * @param value    to set
     * @return true, if successful.
     */
    default boolean compareAndSwapFloat(long offset, float expected, float value)
            throws BufferOverflowException {
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
            throws BufferOverflowException {
        return compareAndSwapLong(offset, Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(value));
    }

    /**
     * @return true if these Bytes use shared memory.
     */
    boolean sharedMemory();

    boolean isDirectMemory();
}
