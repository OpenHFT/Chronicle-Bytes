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
 * Represents a reference to two {@code long} values stored in bytes, providing mechanisms for getting, setting,
 * and manipulating them with various memory ordering effects.
 * <p>
 * This interface extends {@link net.openhft.chronicle.core.values.TwoLongValue} to add the ability to back
 * the two long values with a {@link net.openhft.chronicle.bytes.Bytes} store. This is used for off-heap memory
 * or memory-mapped file storage.
 * <p>
 * Concrete implementations of this interface are responsible for managing the storage and retrieval mechanisms
 * of the two {@code long} values in byte format.
 * <p>
 * Notable methods inherited from {@link net.openhft.chronicle.core.values.TwoLongValue} include:
 * <ul>
 *     <li>{@link net.openhft.chronicle.core.values.TwoLongValue#getValue2()} - Retrieves the second {@code long} value.</li>
 *     <li>{@link net.openhft.chronicle.core.values.TwoLongValue#setValue2(long)} - Sets the second {@code long} value.</li>
 *     <li>{@link net.openhft.chronicle.core.values.TwoLongValue#getVolatileValue2()} - Retrieves the second {@code long} value with volatile semantics.</li>
 *     <li>{@link net.openhft.chronicle.core.values.TwoLongValue#setVolatileValue2(long)} - Sets the second {@code long} value with volatile semantics.</li>
 *     <li>{@link net.openhft.chronicle.core.values.TwoLongValue#addValue2(long)} - Atomically adds the given amount to the second {@code long} value.</li>
 *     <li>{@link net.openhft.chronicle.core.values.TwoLongValue#compareAndSwapValue2(long, long)} - Atomically sets the second {@code long} value if it is equal to the expected value.</li>
 * </ul>
 * <p>
 * Additionally, as an implementation of {@link Byteable}, the classes implementing this interface must provide
 * mechanisms to back the two long values with byte storage.
 * <p>
 * Implementations can also include additional behaviors or optimizations not specified in this interface.
 *
 * @apiNote The {@link net.openhft.chronicle.core.values.TwoLongValue#getValue()} and {@link net.openhft.chronicle.core.values.TwoLongValue#setValue(long)} methods
 * inherited from {@link net.openhft.chronicle.core.values.LongValue} are applicable to the first {@code long} value.
 *
 * @implSpec Implementations must ensure that all methods are thread-safe and that changes to the values are correctly
 * synchronized across threads.
 *
 * @see net.openhft.chronicle.bytes.Byteable
 * @see net.openhft.chronicle.core.values.TwoLongValue
 * @author Peter Lawrey
 */
@SuppressWarnings("rawtypes")
public interface TwoLongReference extends TwoLongValue, Byteable {
    // This interface combines TwoLongValue and Byteable
    // Specific method declarations are not necessary here as they are inherited
}
