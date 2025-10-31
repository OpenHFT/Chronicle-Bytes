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
