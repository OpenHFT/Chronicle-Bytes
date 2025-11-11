/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
