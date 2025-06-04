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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Helpers for checking the state of {@link ReferenceCounted} objects. The class
 * is not instantiable.
 */
public final class ReferenceCountedUtil {

    // Private constructor to prevent instantiation
    private ReferenceCountedUtil() {
    }

    /**
     * Verifies that {@code referenceCounted} still has a positive reference
     * count. If not, {@link ReferenceCounted#releaseLast()} is invoked which
     * will throw a {@link ClosedIllegalStateException} with additional context.
     *
     * @param referenceCounted the resource to test
     * @throws ClosedIllegalStateException if already released
     * @throws NullPointerException if {@code referenceCounted} is null
     */
    public static void throwExceptionIfReleased(final ReferenceCounted referenceCounted) throws ClosedIllegalStateException {
        if (referenceCounted.refCount() <= 0) {
            // Rather than throwing a new ClosedIllegalStateException, we invoke releaseLast() that
            // will provide much more tracing information.
            // Once the ref count reaches zero, this is guaranteed to throw an exception
            referenceCounted.releaseLast();
        }
    }

    /**
     * As {@link #throwExceptionIfReleased(ReferenceCounted)} but only if
     * {@code object} implements {@link ReferenceCounted}. Otherwise a simple
     * null check is performed.
     */
    public static void throwExceptionIfReleased(final Object object) throws ClosedIllegalStateException {
        if (object instanceof ReferenceCounted) {
            throwExceptionIfReleased((ReferenceCounted) object);
        } else {
            requireNonNull(object);
        }
    }
}
