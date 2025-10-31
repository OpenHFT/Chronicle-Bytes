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
 * @see net.openhft.chronicle.bytes.Bytes
 * @see net.openhft.chronicle.bytes.BytesStore
 */
package net.openhft.chronicle.bytes.util;
