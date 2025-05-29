/**
 * Provides references to primitive values and arrays stored directly in a {@link net.openhft.chronicle.bytes.BytesStore}.
 * These references permit low-level manipulation of off-heap or memory-mapped data.
 * The reference objects themselves are not thread-safe.
 *
 * <p>Key classes and interfaces included in this package:
 * <ul>
 *     <li>{@link net.openhft.chronicle.bytes.ref.AbstractReference} - A base class representing a reference to a byte store.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryBooleanReference} - A concrete implementation for reading and writing boolean values in binary format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryIntArrayReference} - Represents a binary array of 32-bit integers.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryIntReference} - Represents a 32-bit integer value in binary form.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryLongArrayReference} - Represents an array of 64-bit values in binary format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryLongReference} - Represents a 64-bit long reference in binary format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryTwoLongReference} - Represents two contiguous 64-bit long references in binary format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.ByteableIntArrayValues} - Interface for a resizable array of integer values in bytes.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.ByteableLongArrayValues} - Interface for byteable long array values that are dynamically sized.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.LongReference} - Represents a reference to a 64-bit long value with byte-level access.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.TextBooleanReference} - Provides a reference to a boolean value in text wire format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.TextIntArrayReference} - Represents a reference to an integer array formatted in text.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.TextIntReference} - Provides a reference to a 32-bit integer in text wire format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.TextLongArrayReference} - Provides a reference to long arrays stored in text format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.TextLongReference} - Provides a reference to a 64-bit long value in Text wire format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.TwoLongReference} - Represents a reference to two contiguous 64-bit long values.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.UncheckedLongReference} - Provides an unchecked reference to a 64-bit long value.</li>
 * </ul>
 *
 * <p>Binary references are suited to latency-sensitive code whereas the text variants
 * exist primarily for debugging. See {@link net.openhft.chronicle.bytes.ref} and the
 * <a href="../adoc/wire-integration.adoc">wire integration guide</a> for usage advice.</p>
 *
 * <p> Prefer binary classes on the hot path; use text classes for observability.
 * @see net.openhft.chronicle.bytes.BytesStore
 * @see net.openhft.chronicle.bytes.Byteable
 */
package net.openhft.chronicle.bytes.ref;
