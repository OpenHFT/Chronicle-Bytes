/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.annotation.Positive;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating {@link MappedBytesStore} instances.
 */
@FunctionalInterface
public interface MappedBytesStoreFactory {

    /**
     * Creates a {@link MappedBytesStore} for the given mapping parameters.
     *
     * @param owner        reference owner for the created store
     * @param mappedFile   parent mapped file
     * @param start        logical start offset within the file
     * @param address      native memory address of the mapping
     * @param capacity     total size of the mapped region
     * @param safeCapacity portion of the region accessible without remapping
     * @param pageSize     page size for alignment checks
     * @return the created store
     * @throws ClosedIllegalStateException if the mapped file has been closed
     */
    @NotNull
    MappedBytesStore create(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, @NonNegative long address, @NonNegative long capacity, @NonNegative long safeCapacity, @Positive int pageSize)
            throws ClosedIllegalStateException;

}
