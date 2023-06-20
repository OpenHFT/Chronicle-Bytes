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
 * This is a marker interface used for serialization purposes. Classes implementing this
 * interface can control whether their instances should be written as self-describing
 * messages.
 *
 * <p>
 * The {@link DontChain} annotation is applied to this interface to indicate that when
 * MethodReaders and MethodWriters are exploring interfaces to implement, this interface
 * should not be considered.
 * </p>
 */
@DontChain
public interface CommonMarshallable {

    /**
     * Determines whether this message should be written as a self-describing message.
     * A self-describing message includes metadata about its type along with the actual data.
     *
     * @return boolean - true if this message should be written as self-describing;
     *                   false otherwise. Default is true.
     */
    default boolean usesSelfDescribingMessage() {
        return true;
    }
}
