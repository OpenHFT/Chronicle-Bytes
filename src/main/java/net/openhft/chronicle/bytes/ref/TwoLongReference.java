/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.values.TwoLongValue;

/**
 * Interface defining a reference to two {@code long} values stored in off-heap memory or memory-mapped files,
 * facilitating direct, efficient manipulation of these values with support for various memory ordering semantics.
 * <p>
 * This interface extends {@link net.openhft.chronicle.core.values.TwoLongValue}, which provides the basic
 * operations for interacting with two separate {@code long} values, and implements {@link net.openhft.chronicle.bytes.Byteable}
 * to allow these values to be backed directly by a {@link net.openhft.chronicle.bytes.Bytes} storage mechanism.
 * <p>
 * Implementations must handle the storage and retrieval of these two long values, ensuring that operations
 * on them respect the semantics of memory ordering prescribed by their method signatures. This interface
 * is particularly useful in low-latency environments where operations directly on raw memory are necessary.
 * <p>
 * Key methods inherited from {@link net.openhft.chronicle.core.values.TwoLongValue} include:
 * <ul>
 *     <li>{@code getValue2()}, {@code setValue2(long)} - Access and update the second {@code long} value.</li>
 *     <li>{@code getVolatileValue2()}, {@code setVolatileValue2(long)} - Volatile read and write operations on the second {@code long} value.</li>
 *     <li>{@code addValue2(long)} - Atomically adds to the second {@code long} value.</li>
 *     <li>{@code compareAndSwapValue2(long, long)} - Attempts to set the second {@code long} value if it matches an expected value.</li>
 * </ul>
 * <p>
 * The methods {@code getValue()} and {@code setValue(long)} from {@link net.openhft.chronicle.core.values.LongValue}
 * apply to the first {@code long} value, providing a consistent interface for operations on both values.
 * <p>
 * Implementations of this interface should ensure thread safety and proper synchronization to maintain consistency
 * across threads, especially in multi-threaded environments. Additional optimizations and behaviors may be
 * implemented but are not mandated by this interface.
 * <p>
 * This documentation should be paired with specific class-level details in concrete implementations to guide
 * developers on the expected performance characteristics and operational specifics.
 *
 * @author Peter Lawrey
 * @see net.openhft.chronicle.bytes.Byteable
 * @see net.openhft.chronicle.core.values.TwoLongValue
 */
@SuppressWarnings("rawtypes")
public interface TwoLongReference extends TwoLongValue, Byteable {
    // This interface combines TwoLongValue and Byteable
    // Specific method declarations are not necessary here as they are inherited
}
