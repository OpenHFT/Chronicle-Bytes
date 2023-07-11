/**
 * Provides classes and interfaces for handling references to arrays of
 * various primitive types with byte representation. This package is a part of
 * the Chronicle Bytes library, which offers high-performance byte manipulation
 * and I/O capabilities.
 *
 * <p>Key classes and interfaces included in this package:
 * <ul>
 *     <li>{@link net.openhft.chronicle.bytes.ref.AbstractReference} - A base class representing a reference to a byte store.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryBooleanReference} - A concrete implementation for reading and writing boolean values in binary format.</li>
 *     <li>{@link net.openhft.chronicle.bytes.ref.BinaryIntArrayReference} - Represents a binary array of 64-bit integers with support for reading, writing, and reference counts.</li>
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
 * <p>This package is mainly used when there is a need for efficient low-level manipulation
 * of arrays and values at the byte level, for instance, when working with memory-mapped files
 * or high-performance I/O.
 *
 * @author OpenHFT
 * @see net.openhft.chronicle.bytes.BytesStore
 * @see net.openhft.chronicle.bytes.Byteable
 */
package net.openhft.chronicle.bytes.ref;
