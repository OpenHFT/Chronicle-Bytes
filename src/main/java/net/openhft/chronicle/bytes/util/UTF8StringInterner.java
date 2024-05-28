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
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.AppendableUtil;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.UTFDataFormatRuntimeException;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

/**
 * A specialized interner for interning strings encoded in UTF-8.
 * <p>
 * This class extends {@link AbstractInterner} and overrides its getValue method to intern
 * strings encoded in UTF-8 format represented in {@link BytesStore}.
 */
public class UTF8StringInterner extends AbstractInterner<String> {

    /**
     * A pool of {@link StringBuilder} instances, used for efficient string construction.
     */
    private static final ScopedResourcePool<StringBuilder> SBP = StringBuilderPool.createThreadLocal(1);

    /**
     * Constructs a new UTF8StringInterner with the specified capacity.
     *
     * @param capacity the maximum number of items that the interner can hold.
     */
    public UTF8StringInterner(@NonNegative int capacity) {
        super(capacity);
    }

    /**
     * Converts the bytes from a {@link BytesStore} into a UTF-8 encoded string.
     * The bytes are assumed to be in UTF-8 format and are decoded accordingly.
     *
     * @param cs     the {@link BytesStore} containing the bytes to be converted
     * @param length the number of bytes to read from the {@link BytesStore}
     * @return the resulting UTF-8 encoded string
     * @throws UTFDataFormatRuntimeException  If the bytes are not valid UTF-8 encoded characters
     * @throws BufferUnderflowException       If the buffer's limits are exceeded
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @SuppressWarnings("rawtypes")
    @Override
    @NotNull
    protected String getValue(@NotNull BytesStore<?, ?> cs, @NonNegative int length)
            throws UTFDataFormatRuntimeException, IllegalStateException, BufferUnderflowException {
        try (final ScopedResource<StringBuilder> sbTl = SBP.get()) {
            // Acquire a StringBuilder from the pool for efficient string construction
            StringBuilder sb = sbTl.get();
            // Parse the bytes as UTF-8 and append them to the StringBuilder
            AppendableUtil.parseUtf8(cs, sb, true, length);
            // Convert the StringBuilder to a string and return it
            return sb.toString();
        }
    }
}
