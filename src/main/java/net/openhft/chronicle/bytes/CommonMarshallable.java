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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;

/**
 * Defines common behavior for marshallable objects, i.e., objects that can be converted to and from
 * a series of bytes. An object of a class implementing this interface can be written as a
 * self-describing message, meaning that it includes metadata about its own structure.
 */
@DontChain
public interface CommonMarshallable {

    /**
     * Determines whether the message produced by this object is self-describing.
     * A self-describing message includes metadata about its structure, which aids
     * in decoding the message without prior knowledge of its structure.
     *
     * @return {@code true} if the message should be self-describing, {@code false} otherwise.
     * By default, this method returns {@code true}.
     */
    default boolean usesSelfDescribingMessage() {
        return true;
    }
}
