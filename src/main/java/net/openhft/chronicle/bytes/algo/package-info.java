/**
 * Provides classes for hash computations on ByteStore objects.
 * <p>
 * This package includes two hash implementations, {@link net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash}
 * and {@link net.openhft.chronicle.bytes.algo.XxHash}. Both implement the {@link net.openhft.chronicle.bytes.algo.BytesStoreHash}
 * interface for the {@link net.openhft.chronicle.bytes.BytesStore} objects. These hashing algorithms are designed for
 * speed and effectiveness. VanillaBytesStoreHash provides a single instance of the hashing algorithm with constants
 * for faster computation, while XxHash is a more customizable hash implementation with seed support.
 * <p>
 * Both classes feature the fetch and applyAsLong methods for fetching values from a byte store and computing their
 * hash respectively. Errors during execution are thrown as IllegalStateException and BufferUnderflowException.
 * <p>
 * {@code XxHash} is migrated from the Zero-Allocation-Hashing project, renowned for its speed.
 * <p>
 * Note: As these algorithms are non-cryptographic, they should not be used for any security or privacy sensitive
 * computations.
 *
 * @see net.openhft.chronicle.bytes.BytesStore
 * @see net.openhft.chronicle.bytes.algo.BytesStoreHash
 */
package net.openhft.chronicle.bytes.algo;
