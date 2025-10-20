/*
 * Copyright 2016-2025 chronicle.software
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
 * Provides factory methods for creating thread-local pools of reusable {@link Bytes} instances.
 * <p>
 * The {@link ScopedResourcePool} returned by {@link #createThreadLocal()} manages the lifecycle of
 * {@link Bytes} objects per thread to minimise allocation overhead.
 * See {@code pool-overview.adoc} for tuning guidelines.
 */
public final class BytesPool {

    /** Default number of {@link Bytes} instances cached per thread. */
    private static final int DEFAULT_BYTES_POOL_SIZE_PER_THREAD =
            Jvm.getInteger("chronicle.bytesPool.instancesPerThread", 4);

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
     * Used by legacy code paths that do not employ {@link ScopedResourcePool}.
     */
    final ThreadLocal<Bytes<?>> bytesTL = new ThreadLocal<>();

    /**
     * Creates a new {@link Bytes} instance for use by the pool.
     * Invoked when no cached instance is available for the current thread.
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
