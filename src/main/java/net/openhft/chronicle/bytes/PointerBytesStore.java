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

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

/**
 * A BytesStore which can point to arbitrary memory.
 * Acts as a view of Bytes over an area of memory.
 * Recommended not to use this in conjunction with ElasticBytes as underlying data structure may move.
 */
public class PointerBytesStore extends NativeBytesStore<Void> {

    public PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false, false);
    }

    public void set(long address, @NonNegative long capacity) {
        setAddress(address);
        this.limit = maximumLimit = capacity;
        if (capacity == Bytes.MAX_CAPACITY)
            Jvm.warn().on(getClass(), "the provided capacity of underlying looks like it may have come " +
                    "from an elastic bytes, please make sure you do not use PointerBytesStore with " +
                    "ElasticBytes since the address of the underlying store may change once it expands");
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite()
            throws IllegalStateException {
        try {
            return new VanillaBytes<>(this, 0, Bytes.MAX_CAPACITY);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @NonNegative
    @Override
    public long safeLimit() {
        return limit;
    }

    @NonNegative
    @Override
    public long start() {
        return 0;
    }
}
