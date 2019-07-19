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

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.BufferOverflowException;

/**
 * @author Rob Austin.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesRingBuffer extends BytesRingBufferStats, BytesConsumer, Closeable {

    Logger LOG = LoggerFactory.getLogger(BytesRingBuffer.class);

    @NotNull
    static BytesRingBuffer newInstance(@NotNull NativeBytesStore<Void> bytesStore) {
        return newInstance(bytesStore, 1);
    }

    @NotNull
    static MultiReaderBytesRingBuffer newInstance(
            @NotNull NativeBytesStore<Void> bytesStore,
            int numReaders) {
        try {
            @NotNull final Class<MultiReaderBytesRingBuffer> aClass = clazz();
            final Constructor<MultiReaderBytesRingBuffer> constructor = aClass
                    .getDeclaredConstructor(BytesStore.class, int.class);
            return constructor.newInstance(bytesStore, numReaders);

        } catch (Exception e) {
            LOG.error("This is a a commercial feature, please contact " +
                    "sales@higherfrequencytrading.com to unlock this feature.");

            throw Jvm.rethrow(e);
        }
    }

    @NotNull
    static Class<MultiReaderBytesRingBuffer> clazz() throws ClassNotFoundException {
        //noinspection AccessStaticViaInstance
        return (Class<MultiReaderBytesRingBuffer>) Class.forName(
                "software.chronicle.enterprise.ring.EnterpriseRingBuffer");
    }

    static long sizeFor(long capacity) {
        return sizeFor(capacity, 1);
    }

    static long sizeFor(long capacity, int numReaders) {
        try {
            //noinspection AccessStaticViaInstance
            final Method sizeFor = Class.forName(
                    "software.chronicle.enterprise.queue.ChronicleRingBuffer").getMethod("sizeFor", long.class, int.class);
            return (long) sizeFor.invoke(null, capacity, numReaders);

        } catch (Exception e) {
            LOG.error("This is a a commercial feature, please contact " +
                    "sales@higherfrequencytrading.com to unlock this feature.");

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
    boolean read(@NotNull BytesOut using);

    long readRemaining();

    boolean isEmpty();

    BytesStore bytesStore();
}
