/**
 * Provides versatile, high-performance access to contiguous regions of memory.
 * Implementations cover on-heap arrays, native memory and memory-mapped files.
 * The API forms a building block for other Chronicle libraries.
 *
 * <p>Key features:
 * <ul>
 * <li>Support for 63-bit addressing for large structures.</li>
 * <li>Efficient encoding and decoding for UTF-8 and ISO-8859-1 strings.</li>
 * <li>Thread-safe atomic operations via {@link net.openhft.chronicle.bytes.BytesStore BytesStore}.</li>
 * <li>Deterministic resource management using
 * {@link net.openhft.chronicle.core.io.ReferenceCounted ReferenceCounted}.</li>
 * <li>Elastic buffers such as {@link net.openhft.chronicle.bytes.Bytes#allocateElasticDirect()}.</li>
 * <li>Direct number parsing and formatting to and from byte sequences.</li>
 * </ul>
 *
 * Core abstractions:
 * <ul>
 * <li>{@link net.openhft.chronicle.bytes.BytesStore BytesStore} &ndash; a fixed-size region of memory.</li>
 * <li>{@link net.openhft.chronicle.bytes.Bytes Bytes} &ndash; a mutable view with independent
 * read and write cursors.</li>
 * </ul>
 */
package net.openhft.chronicle.bytes;
