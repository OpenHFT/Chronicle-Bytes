/**
 * This package provides a pool for managing {@link net.openhft.chronicle.bytes.Bytes} instances.
 * <p>
 * {@link net.openhft.chronicle.bytes.pool.BytesPool} exposes factory methods
 * such as {@link net.openhft.chronicle.bytes.pool.BytesPool#createThreadLocal()}
 * that create thread-local caches of {@link net.openhft.chronicle.bytes.Bytes}
 * objects. Pooling helps reduce allocation churn even with modern garbage
 * collectors when latencies of only a few micro-seconds are required.
 * <p>
 * See the AsciiDoc module overview {@code pool-overview.adoc} for configuration
 * examples and tuning hints.
 *
 */
package net.openhft.chronicle.bytes.pool;
