/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * Represents a ring buffer for {@link Bytes} data, intended for
 * high-throughput, low-latency messaging between threads or services. It
 * combines statistics ({@link BytesRingBufferStats}), byte consumption
 * ({@link BytesConsumer}) and resource management ({@link Closeable}). Direct
 * user implementation is discouraged and some functionality may require
 * commercial libraries.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesRingBuffer extends BytesRingBufferStats, BytesConsumer, Closeable {
    /**
     * Factory method to create a new ring buffer.
     *
     * @param bytesStore backing store
     * @return new ring buffer instance
     */
    @NotNull
    static BytesRingBuffer newInstance(@NotNull BytesStore<?, Void> bytesStore) {
        return newInstance(bytesStore, 1);
    }

    /**
     * Factory method to create a ring buffer with multiple readers.
     *
     * @param bytesStore backing store
     * @param numReaders number of readers
     * @return new {@link MultiReaderBytesRingBuffer}
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
     * @return the {@link Class} for the commercial implementation of
     *         {@link MultiReaderBytesRingBuffer}
     * @throws ClassNotFoundException if the implementation class is not present
     */
    @NotNull
    static Class<MultiReaderBytesRingBuffer> clazz()
            throws ClassNotFoundException {
        return (Class<MultiReaderBytesRingBuffer>) Class.forName(
                "software.chronicle.enterprise.ring.EnterpriseRingBuffer");
    }

    /**
     * Calculates the total byte size required for a ring buffer of the given
     * capacity.
     */
    static long sizeFor(@NonNegative long capacity) {
        return sizeFor(capacity, 1);
    }

    /**
     * Calculates the total byte size required for a ring buffer with the given
     * {@code capacity} and number of readers.
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
     * Clears the buffer, typically by advancing reader positions to the current
     * write position.
     */
    void clear();

    /**
     * Attempts to write the content of {@code bytes0} as a single message.
     *
     * @param bytes0 bytes to write
     * @return {@code true} if the message was written, {@code false} if the
     *         buffer lacks space
     */
    boolean offer(@NotNull BytesStore<?, ?> bytes0);

    /**
     * Reads the next available message into {@code using}.
     *
     * @param using destination buffer
     * @return {@code true} if a message was read, {@code false} if none were available
     * @throws BufferOverflowException if {@code using} is too small
     */
    @Override
    boolean read(@NotNull BytesOut<?> using);

    /**
     * Number of bytes currently available for reading from the default reader
     * perspective.
     */
    long readRemaining();

    /**
     * @return {@code true} if no readable messages are present for the default
     *         reader
     */
    boolean isEmpty();

    /**
     * Retrieves the BytesStore that backs this buffer. The returned BytesStore
     * provides access to the bytes contained within the buffer, allowing them to be read or written.
     *
     * @return the BytesStore backing this buffer
     */
    BytesStore<?, ?> bytesStore();
}
