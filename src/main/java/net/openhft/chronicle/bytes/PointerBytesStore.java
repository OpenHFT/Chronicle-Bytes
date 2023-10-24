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
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

/**
 * A BytesStore that points to arbitrary memory.
 * <p>
 * This class represents a view of Bytes over an arbitrary area of memory. It provides methods to set and
 * interact with this area of memory.
 *
 * <p><b>WARNING:</b> It is not recommended to use this in conjunction with ElasticBytes. ElasticBytes, by design,
 * can change its underlying data structure's location, which might invalidate or corrupt the memory view
 * held by this PointerBytesStore.
 */
public class PointerBytesStore extends NativeBytesStore<Void> {

    /**
     * Default constructor that initializes a PointerBytesStore with no data.
     */
    public PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false, false);
    }

    /**
     * Sets the memory address and capacity of this PointerBytesStore.
     *
     * @param address  the memory address
     * @param capacity the size of the memory to which this PointerBytesStore should point
     * @throws IllegalArgumentException if the capacity is negative
     */
    public void set(long address, @NonNegative long capacity) throws IllegalArgumentException {
        setAddress(address);
        this.limit = maximumLimit = capacity;
        if (capacity == Bytes.MAX_CAPACITY)
            Jvm.warn().on(getClass(), "the provided capacity of underlying looks like it may have come " +
                    "from an elastic bytes, please make sure you do not use PointerBytesStore with " +
                    "ElasticBytes since the address of the underlying store may change once it expands");
    }

    /**
     * Returns a new VanillaBytes for writing to this PointerBytesStore.
     *
     * @return a new VanillaBytes
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite()
            throws IllegalStateException {
        return new VanillaBytes<>(this, 0, Bytes.MAX_CAPACITY);
    }

    /**
     * Returns the safe limit of the memory to which this PointerBytesStore can write or read.
     *
     * @return the safe limit
     */
    @NonNegative
    @Override
    public long safeLimit() {
        return limit;
    }

    /**
     * Returns the starting address of the memory to which this PointerBytesStore points.
     *
     * @return the start address, always 0 in the case of PointerBytesStore
     */
    @NonNegative
    @Override
    public long start() {
        return 0;
    }
}
