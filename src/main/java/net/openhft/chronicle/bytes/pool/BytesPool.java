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
package net.openhft.chronicle.bytes.pool;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import net.openhft.chronicle.core.scoped.ScopedThreadLocal;
import org.jetbrains.annotations.NotNull;

/**
 * A thread-local pool of reusable {@link Bytes} instances.
 * <p>
 * This class uses a {@link ThreadLocal} to store a single {@link Bytes} instance per thread,
 * which can be reused to avoid the overhead of creating a new instance every time bytes are
 * needed for operations.
 * <p>
 * This class is primarily meant to be used in high-performance environments where reducing
 * object creation is crucial.
 */
public final class BytesPool {

    private static final int DEFAULT_BYTES_POOL_SIZE_PER_THREAD = Jvm.getInteger("chronicle.bytesPool.instancesPerThread", 4);

    @Deprecated(/* To be removed in x.26 */)
    public BytesPool() {
    }

    /**
     * Create a scoped-thread-local pool of bytes resources
     *
     * @return The pool
     */
    public static ScopedResourcePool<Bytes<?>> createThreadLocal() {
        return createThreadLocal(DEFAULT_BYTES_POOL_SIZE_PER_THREAD);
    }

    /**
     * Create a scoped-thread-local pool of bytes resources
     *
     * @param instancesPerThread The maximum number of instances to retain per thread
     * @return The pool
     */
    public static ScopedResourcePool<Bytes<?>> createThreadLocal(int instancesPerThread) {
        return new ScopedThreadLocal<>(
                BytesPool::createBytes,
                Bytes::clear,
                instancesPerThread);
    }

    /**
     * Thread-local variable that holds the {@link Bytes} instance for each thread.
     */
    final ThreadLocal<Bytes<?>> bytesTL = new ThreadLocal<>();

    /**
     * Acquires a {@link Bytes} instance from the thread-local pool. If the pool does not
     * contain a {@link Bytes} instance for the current thread, a new one is created and
     * added to the pool.
     *
     * @return A {@link Bytes} instance.
     * @deprecated Use {@link BytesPool#createThreadLocal()} and {@link ScopedResourcePool#get()} instead
     */
    @Deprecated(/* To be removed in x.26 */)
    public Bytes<?> acquireBytes() {
        Bytes<?> bytes = bytesTL.get();
        if (bytes != null) {
            try {
                return bytes.clear();
            } catch (IllegalStateException e) {
                // ignored
            }
        } else {
            bytes = createBytes();
            bytesTL.set(bytes);
        }
        return bytes;
    }

    /**
     * Creates a new {@link Bytes} instance.
     * <p>
     * This method is called internally when there is no {@link Bytes} instance available
     * in the thread-local pool for the current thread.
     *
     * @return A newly created {@link Bytes} instance.
     */
    @NotNull
    private static Bytes<?> createBytes() {
        Bytes<?> bbb = Bytes.allocateElasticDirect(256);
        IOTools.unmonitor(bbb);
        return bbb;
    }
}
