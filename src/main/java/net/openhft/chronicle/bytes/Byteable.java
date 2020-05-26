/*
 * Copyright 2016-2020 Chronicle Software
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

import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * This Interface allows a reference to off heap memory to be reassigned.
 * <p></p>
 * A reference to off heap memory is a proxy for some memory which sits outside the heap.
 */
public interface Byteable<B extends BytesStore<B, Underlying>, Underlying> {
    /**
     * This setter for a data type which points to an underlying ByteStore.
     *
     * @param bytesStore the fix point ByteStore
     * @param offset     the offset within the ByteStore
     * @param length     the length in the ByteStore
     */
    void bytesStore(BytesStore<B, Underlying> bytesStore, long offset, long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException,
            BufferUnderflowException;

    @Nullable
    BytesStore<B, Underlying> bytesStore();

    long offset();

    /**
     * @return the maximum size in byte for this reference.
     */
    long maxSize();
}