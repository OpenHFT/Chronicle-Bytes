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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.values.TwoLongValue;

/**
 * Reference to two contiguous 64-bit values.
 *
 * <p>The interface itself does not prescribe thread-safety; the
 * implementation decides.</p>
 *
 * @see BinaryTwoLongReference
 */
@SuppressWarnings("rawtypes")
public interface TwoLongReference extends TwoLongValue, Byteable {
    // This interface combines TwoLongValue and Byteable
    // Specific method declarations are not necessary here as they are inherited
}
