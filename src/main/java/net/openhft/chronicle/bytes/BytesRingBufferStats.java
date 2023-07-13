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

import java.util.List;

/**
 * This interface provides statistics about a {@link BytesRingBuffer}.
 */
public interface BytesRingBufferStats {
    /**
     * Each time the ring is read, the number of bytes remaining in the write buffer are recorded, and that
     * number is returned by this method.
     * Calling this method resets the number. If no read calls were made since the last
     * call to this method, it returns Long.MAX_VALUE.
     *
     * @return the number of bytes remaining in the write buffer as at the last read
     * or Long.MAX_VALUE if no reads were performed.
     */
    @NonNegative
    long minNumberOfWriteBytesRemaining();

    /**
     * @return the total capacity of the ring buffer in bytes.
     */
    @NonNegative
    long capacity();

    /**
     * Calling this method resets the number.
     *
     * @return the number of write operations performed since the last call to this method.
     */
    @NonNegative
    long getAndClearWriteCount();

    /**
     * Calling this method resets the number.
     *
     * @return the number of missed write operations since the last call to this method.
     */
    @NonNegative
    long getAndClearMissedWriteCount();

    /**
     * Calling this method resets the number.
     *
     * @return the number of contentions since the last call to this method.
     */
    @NonNegative
    long getAndClearContentionCount();

    /**
     * @return a list of {@link RingBufferReaderStats} objects, each representing the statistics
     * for a reader of the ring buffer.
     */
    List<RingBufferReaderStats> readers();
}
