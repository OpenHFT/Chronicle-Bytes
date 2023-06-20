/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.Closeable;
/**
 * An interface for a reader on a Ring Buffer, providing methods to read and navigate through the buffer.
 * The reader supports a read-once-and-discard paradigm which makes it suitable for situations where
 * high throughput is required and old data is irrelevant.
 *
 * This interface also extends {@link RingBufferReaderStats}, which provides statistics
 * about the Ring Buffer's usage, and {@link Closeable} for closing the reader when it's no longer needed.
 */
public interface RingBufferReader extends RingBufferReaderStats, Closeable {

    /**
     * Represents an undefined or unknown index within the ring buffer.
     */
    long UNKNOWN_INDEX = -1;

    /**
     * @return true if the Ring Buffer is empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * @return true if the Ring Buffer reader has been stopped, false otherwise.
     */
    boolean isStopped();

    /**
     * Stops the reader. After being stopped, the reader will not block writers. The reader can be re-opened after being stopped.
     */
    void stop();

    /**
     * Prepares the reader to read data from the Ring Buffer. This method adjusts the read position and read limit
     * in the provided {@link Bytes} object, allowing the client to read the data.
     *
     * @param bytes The {@link Bytes} instance backed by the Ring Buffer.
     * @return The position from where next read operation should be performed, this should be passed to
     * {@link RingBufferReader#afterRead(long)} after the read operation.
     */
    @NonNegative
    long beforeRead(Bytes<?> bytes);

    /**
     * Updates the reader's state after a read operation. The parameter is usually the return value of the preceding {@link #beforeRead(Bytes)} call.
     *
     * @param next The position after the last read operation.
     */
    void afterRead(@NonNegative long next);

    /**
     * Overloaded method of {@link #afterRead(long)} providing additional details about the read operation.
     *
     * @param next The position after the last read operation.
     * @param payloadStart The starting position of the payload that was read.
     * @param underlyingIndex The index in the underlying data structure from where the data was read.
     */
    void afterRead(@NonNegative long next, long payloadStart, long underlyingIndex);

    /**
     * @return The index in the underlying data structure where the last read operation was performed.
     */
    long underlyingIndex();

    /**
     * A convenience method that reads data from the Ring Buffer by internally calling both {@link #beforeRead(Bytes)} and {@link #afterRead(long)}.
     *
     * @return True if the read operation succeeded, false otherwise.
     */
    boolean read(BytesOut<?> bytes);

    /**
     * @return The {@link BytesStore} instance which backs the Ring Buffer.
     */
    @SuppressWarnings("rawtypes")
    BytesStore byteStore();

    /**
     * Adjusts the reader's position to just past the end of the Ring Buffer, effectively making it read any new data that gets written.
     */
    void toEnd();
}
