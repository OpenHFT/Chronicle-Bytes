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

/**
 * Functional interface representing a parselet for processing byte data.
 * The implementing class should provide the logic to accept and handle
 * a certain type of message represented in bytes.
 */
@FunctionalInterface
public interface BytesParselet {
    /**
     * This method is invoked with a message type and an input stream of bytes.
     * The implementing class is expected to parse and process the bytes according
     * to the provided message type.
     *
     * @param messageType a long value representing the type of the message to be processed.
     * @param in          a {@link BytesIn} instance containing the message data in bytes.
     */
    void accept(long messageType, BytesIn<?> in);
}
