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
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface that serves as a factory for creating instances of {@link MappedBytesStore}.
 * <p>
 * This interface is meant to be implemented by classes that can create MappedBytesStore instances based on provided parameters.
 */
@FunctionalInterface
public interface MappedBytesStoreFactory {

    /**
     * Creates a {@link MappedBytesStore} instance with the given parameters.
     *
     * @param owner        The owner of the MappedBytesStore to be created.
     * @param mappedFile   The MappedFile to be wrapped by the created BytesStore.
     * @param start        The start position within the MappedFile.
     * @param address      The memory address of the mapped data.
     * @param capacity     The capacity of the mapped data.
     * @param safeCapacity The safe capacity of the mapped data. Accessing data beyond the safe capacity might lead to a crash.
     * @return The created MappedBytesStore instance.
     * @throws IllegalStateException If the MappedFile has already been released.
     */
    @NotNull
    MappedBytesStore create(ReferenceOwner owner, MappedFile mappedFile, @NonNegative long start, @NonNegative long address, @NonNegative long capacity, @NonNegative long safeCapacity)
            throws IllegalStateException;
}
