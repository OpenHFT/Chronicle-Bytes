/*
 * Copyright 2016-2025 chronicle.software
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
