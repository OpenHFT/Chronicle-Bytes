/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.io.IORuntimeException;

import java.nio.BufferUnderflowException;

/**
 * Read data directly as Bytes.
 */
@FunctionalInterface
@DontChain
public interface ReadBytesMarshallable extends CommonMarshallable {
    /**
     * Bytes to read.  This can be used as a method to implement or as a lambda.
     *
     * @param bytes to read.
     */
    @SuppressWarnings("rawtypes")
    void readMarshallable(BytesIn<?> bytes)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException;
}
