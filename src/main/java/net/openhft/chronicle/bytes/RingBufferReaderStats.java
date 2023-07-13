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

/**
 * An interface to provide statistics about a {@link RingBufferReader}'s reading operations.
 * This includes the number of successful reads, missed reads and how far behind the reader is.
 */
public interface RingBufferReaderStats {

    /**
     * Retrieves and resets the count of successful read operations performed by the RingBufferReader.
     * Calling this method resets the number.
     *
     * @return The number of successful read operations since the last call to this method.
     */
    @NonNegative
    long getAndClearReadCount();

    /**
     * Retrieves and resets the count of missed read operations performed by the RingBufferReader.
     * Missed reads occur if there was no new data to be read.
     * Calling this method resets the number.
     *
     * @return The number of missed read operations since the last call to this method.
     */
    @NonNegative
    long getAndClearMissedReadCount();

    /**
     * Calculates how far behind the RingBufferReader is relative to the write position in the Ring Buffer.
     * This provides an indication of how much unread data remains in the buffer for this reader.
     *
     * @return The number of bytes yet to be read by the RingBufferReader.
     */
    @NonNegative
    long behind();
}
