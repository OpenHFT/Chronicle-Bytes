/**
 * Provides classes for non-cryptographic hash computations on {@link net.openhft.chronicle.bytes.BytesStore} objects.
 * <p>
 * The package offers three implementations:
 * {@link net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash},
 * {@link net.openhft.chronicle.bytes.algo.XxHash} and
 * {@link net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash}. They all
 * implement {@link net.openhft.chronicle.bytes.algo.BytesStoreHash}.
 * VanillaBytesStoreHash uses a fixed set of mixing constants for quick hashing
 * while XxHash supports a seed. OptimisedBytesStoreHash chooses the most
 * efficient strategy based on size and direct memory access.
 * <p>
 * {@code XxHash} was migrated from the Zero-Allocation-Hashing project and is
 * renowned for speed. These algorithms are deterministic across platforms but
 * should not be used for security or privacy-sensitive computations.
 * <p>
 * See the AsciiDoc module overview {@code algo-overview.adoc} for examples and
 * performance notes.
 *
 * @see net.openhft.chronicle.bytes.BytesStore
 * @see net.openhft.chronicle.bytes.algo.BytesStoreHash
 * @see <a href="../../../../src/main/docs/algo-overview.adoc">algo-overview.adoc</a>
 */
package net.openhft.chronicle.bytes.algo;
