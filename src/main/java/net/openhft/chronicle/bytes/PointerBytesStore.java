/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * {@link NativeBytesStore} providing a view over externally managed memory.
 * Not suitable for elastic bytes.
 */
public class PointerBytesStore extends NativeBytesStore<Void> {

    /**
     * Constructs a store with no associated memory.
     */
    public PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false, false);
    }

    /**
     * Points this store at a new memory region.
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
