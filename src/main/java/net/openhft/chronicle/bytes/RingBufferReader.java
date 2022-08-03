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

public interface RingBufferReader extends RingBufferReaderStats, Closeable {
    long UNKNOWN_INDEX = -1;

    boolean isEmpty();

    boolean isStopped();

    /**
     * stop the reader. After being stopped, the reader will not block writers.
     * After being stopped the reader can be re-opened
     */
    void stop();

    /**
     * the readPosition and readLimit will be adjusted so that the client can read the data
     *
     * @param bytes who's byteStore must be the ring buffer,
     * @return nextReadPosition which should be passed to {@link RingBufferReader#afterRead(long)}
     */
    @SuppressWarnings("rawtypes")
    @NonNegative
    long beforeRead(Bytes<?> bytes);

    void afterRead(@NonNegative long next);

    void afterRead(@NonNegative long next, long payloadStart, long underlyingIndex);

    long underlyingIndex();

    /**
     * Convenience method calls both {@link #beforeRead(Bytes)} and {@link #afterRead(long)}
     *
     * @return whether read succeeded
     */
    @SuppressWarnings("rawtypes")
    boolean read(BytesOut<?> bytes);

    /**
     * @return the byteStore which backs the ring buffer
     */
    @SuppressWarnings("rawtypes")
    BytesStore byteStore();

    /**
     * Take reader to just past the end of the RB
     */
    void toEnd();
}
