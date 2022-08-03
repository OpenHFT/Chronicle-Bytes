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

import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SubBytes<U> extends VanillaBytes<U> {
    private final long start;
    private final long capacity;

    /**
     * Class constructor. Creates a SubBytes from the bytes in a specified BytesStore from a specified Offset to
     * a specified index (excluding).
     *
     * @param bytesStore the specified BytesStore used to create this SubBytes
     * @param start      the offset of bytesStore
     * @param capacity   the last index (excluding) of bytesStore to be included in this SubBytes
     * @throws IllegalStateException if bytesStore is released
     * @throws BufferUnderflowException if capacity is less than start
     * @throws IllegalArgumentException
     */
    public SubBytes(@NotNull BytesStore bytesStore, @NonNegative long start, @NonNegative long capacity)
            throws IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        super(bytesStore);
        this.start = start;
        this.capacity = capacity;
        clear();
        readLimit(writeLimit());
    }

    @NonNegative
    @Override
    public long capacity() {
        return capacity;
    }

    @NonNegative
    @Override
    public long start() {
        return start;
    }

    @NonNegative
    @Override
    public long realCapacity() {
        return capacity;
    }
}
