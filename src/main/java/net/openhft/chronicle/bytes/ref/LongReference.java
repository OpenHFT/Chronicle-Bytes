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
import net.openhft.chronicle.core.values.LongValue;

/**
 * Represents a reference to a 64-bit long value, which is both {@link Byteable} and {@link LongValue}.
 *
 * <p>The {@code LongReference} interface provides a contract for classes that need to represent
 * a reference to a long value which can be read from or written to a {@link net.openhft.chronicle.bytes.BytesStore}.
 * This interface is particularly useful for memory-mapped values, where changes to the value are reflected in memory.</p>
 *
 * <p>Classes implementing this interface are expected to provide efficient, low-level access
 * to the underlying bytes of the referenced long value.</p>
 *
 * @see Byteable
 * @see LongValue
 */
@SuppressWarnings("rawtypes")
public interface LongReference extends LongValue, Byteable {
    // This interface combines LongValue and Byteable
    // Specific method declarations are not necessary here as they are inherited
}
