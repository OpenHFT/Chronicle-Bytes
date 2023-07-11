/**
 * This package provides a pool for managing {@link net.openhft.chronicle.bytes.Bytes} instances.
 * <p>
 * The {@link net.openhft.chronicle.bytes.pool.BytesPool} class is used for pooling {@link net.openhft.chronicle.bytes.Bytes}
 * instances to reduce the overhead of frequently creating new instances. It uses a {@link java.lang.ThreadLocal} storage to ensure
 * that each thread has its own instance of {@link net.openhft.chronicle.bytes.Bytes}.
 * 
 */
package net.openhft.chronicle.bytes.pool;
