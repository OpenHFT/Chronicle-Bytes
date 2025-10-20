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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;

/**
 * Marker for objects that can be serialised to bytes.  Implementations may choose to embed
 * type or structural metadata so that the resulting message is self describing.
 */
@DontChain
public interface CommonMarshallable {

    /**
     * Indicates whether the serialised form should contain enough metadata for a generic parser to
     * understand it without prior knowledge of the concrete type.
     *
     * @return {@code true} by default
     */
    default boolean usesSelfDescribingMessage() {
        return true;
    }
}
