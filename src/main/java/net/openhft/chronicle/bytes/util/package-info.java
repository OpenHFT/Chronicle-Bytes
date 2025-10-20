/**
 * Internal helper classes and interfaces used by Chronicle Bytes.
 * <p>
 * These utilities follow Chronicle's zero-allocation, low-latency design philosophy
 * and may change between minor releases. They live outside the public API and
 * carry no binary-compatibility guarantees. Features include:
 * 
 * <ul>
 *     <li>Specialised exceptions for buffer overflow and underflow, allowing custom messages.</li>
 *     <li>Interning utilities for strings in various character encodings, and utilities for
 *         interned objects in general, which help in reducing memory usage.</li>
 *     <li>Utilities for property replacement within strings based on property values.</li>
 *     <li>Support for escaping stop characters in character sequences, useful for parsing
 *         and tokenisation tasks.</li>
 *     <li>Compression and decompression utilities for working with byte data.</li>
 * </ul>
 * <p>
 * This package forms part of Chronicle Bytes which is optimised for low level I/O,
 * serialisation and data manipulation.
 * <p>
 * Note: Most classes are not thread safe unless stated otherwise.
 * 
 *
 * @see net.openhft.chronicle.bytes.Bytes
 * @see net.openhft.chronicle.bytes.BytesStore
 */
package net.openhft.chronicle.bytes.util;
