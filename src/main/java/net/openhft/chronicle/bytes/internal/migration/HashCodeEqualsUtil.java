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
package net.openhft.chronicle.bytes.internal.migration;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;

public final class HashCodeEqualsUtil {

    private HashCodeEqualsUtil() {
    }

    public static int hashCode(final @NotNull BytesStore<?, ?> bytes) {
        // Reserving prevents illegal access to this Bytes object if released by another thread
        final ReferenceOwner owner = ReferenceOwner.temporary("hashCode");
        bytes.reserve(owner);
        try {
            return BytesStoreHash.hash32(bytes);
        } finally {
            bytes.release(owner);
        }
    }

}
