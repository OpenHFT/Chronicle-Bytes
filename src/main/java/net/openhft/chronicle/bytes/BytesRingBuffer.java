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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.BufferOverflowException;

/**
 * This interface represents a ring buffer data structure capable of reading and writing
 * Bytes (binary data). The BytesRingBuffer interface extends {@link BytesRingBufferStats},
 * {@link BytesConsumer}, and {@link Closeable} to provide statistics about the ring buffer,
 * consume bytes from the buffer and close the buffer when it's no longer needed.
 *
 * <p>This interface also includes methods for creating instances of ring buffer, determining the size,
 * checking for emptiness, and offering or reading bytes to/from the buffer.</p>
 *
 * <p>Note that some methods in this interface are expected to be implemented in commercial versions
 * and would need unlocking for use.</p>
 *
 * <p>This interface is not meant to be implemented by user code.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesRingBuffer extends BytesRingBufferStats, BytesConsumer, Closeable {
    /**
     * Constructs a new  instance with the provided {@link BytesStore}.
     *
     * @param bytesStore the {@link BytesStore} to be used for the ring buffer.
     * @return a new instance of .
     */
    @NotNull
    static BytesRingBuffer newInstance(@NotNull BytesStore<?, Void> bytesStore) {
        return newInstance(bytesStore, 1);
    }

    /**
     * Constructs a new {@link MultiReaderBytesRingBuffer} instance with the provided {@link BytesStore} and a given number of readers.
     *
     * @param bytesStore the {@link BytesStore} to be used for the ring buffer.
     * @param numReaders the number of readers for the ring buffer.
     * @return a new instance of {@link MultiReaderBytesRingBuffer}.
     */
    @NotNull
    static MultiReaderBytesRingBuffer newInstance(
            @NotNull BytesStore<?, Void> bytesStore,
            @NonNegative int numReaders) {
        try {
            @NotNull final Class<MultiReaderBytesRingBuffer> aClass = clazz();
            final Constructor<MultiReaderBytesRingBuffer> constructor = aClass
                    .getDeclaredConstructor(BytesStore.class, int.class);
            return constructor.newInstance(bytesStore, numReaders);

        } catch (Exception e) {
            Jvm.error().on(BytesRingBuffer.class,
                    "This is a a commercial feature, please contact " +
                            "sales@chronicle.software to unlock this feature.");

            throw Jvm.rethrow(e);
        }
    }

    /**
     * Returns the {@link Class} object for {@link MultiReaderBytesRingBuffer}.
     *
     * @return the {@link Class} object for {@link MultiReaderBytesRingBuffer}.
     * @throws ClassNotFoundException if the class "software.chronicle.enterprise.ring.EnterpriseRingBuffer" is not found.
     */
    @NotNull
    static Class<MultiReaderBytesRingBuffer> clazz()
            throws ClassNotFoundException {
        return (Class<MultiReaderBytesRingBuffer>) Class.forName(
                "software.chronicle.enterprise.ring.EnterpriseRingBuffer");
    }

    /**
     * Calculates the required size for the ring buffer with a given capacity.
     *
     * @param capacity the capacity of the ring buffer.
     * @return the required size for the ring buffer.
     */
    static long sizeFor(@NonNegative long capacity) {
        return sizeFor(capacity, 1);
    }

    /**
     * Calculates the required size for the ring buffer with a given capacity and a specific number of readers.
     *
     * @param capacity   the capacity of the ring buffer.
     * @param numReaders the number of readers for the ring buffer.
     * @return the required size for the ring buffer.
     */
    static long sizeFor(@NonNegative long capacity, @NonNegative int numReaders) {
        try {
            final Method sizeFor = Class.forName(
                    "software.chronicle.enterprise.queue.ChronicleRingBuffer").getMethod("sizeFor", long.class, int.class);
            return (long) sizeFor.invoke(null, capacity, numReaders);

        } catch (Exception e) {
            Jvm.error().on(BytesRingBuffer.class,
                    "This is a a commercial feature, please contact " +
                            "sales@chronicle.software to unlock this feature.");

            throw Jvm.rethrow(e);
        }
    }

    /**
     * clears the ring buffer but moving the read position to the write position
     */
    void clear();

    /**
     * Inserts the specified element at the tail of this queue if it is possible to do so
     * immediately without exceeding the queue's capacity,
     *
     * @param bytes0 the {@code bytes0} that you wish to add to the ring buffer
     * @return returning {@code true} upon success and {@code false} if this queue is full.
     */
    boolean offer(@NotNull BytesStore bytes0);

    /**
     * Retrieves and removes the head of this queue, or returns {@code null} if this queue is
     * empty.
     *
     * @param using Bytes to read into.
     * @return false if this queue is empty, or a populated buffer if the element was retried
     * @throws BufferOverflowException is the {@code using} buffer is not large enough
     */
    @Override
    boolean read(@NotNull BytesOut<?> using);

    long readRemaining();

    boolean isEmpty();

    BytesStore bytesStore();
}
