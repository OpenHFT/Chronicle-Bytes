package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public final class ReferenceCountedUtil {

    private ReferenceCountedUtil() {
    }

    /**
     * Checks the provided {@code referenceCounted} throwing a ClosedIllegalStateException if it has been previously released.
     *
     * @param referenceCounted non-null resource to check
     * @throws ClosedIllegalStateException if the provided {@code referenceCounted} is released
     * @throws NullPointerException        if the provided {@code referenceCounted} is {@code null}
     */
    public static void throwExceptionIfReleased(final ReferenceCounted referenceCounted) {
        if (referenceCounted.refCount() <= 0) {
            // Rather than throwing a new ClosedIllegalStateException, we invoke releaseLast() that
            // will provide much more tracing information.
            // Once the ref count reaches zero, this is guaranteed to throw an exception
            referenceCounted.releaseLast();
        }
    }

    /**
     * Checks the provided {@code object} throwing a ClosedIllegalStateException if it implements
     * ReferenceCounted AND has been previously released.
     *
     * @param object non-null resource to check
     * @throws ClosedIllegalStateException if the provided {@code object} is released
     * @throws NullPointerException        if the provided {@code object} is {@code null}
     */
    public static void throwExceptionIfReleased(final Object object) {
        if (object instanceof ReferenceCounted) {
            throwExceptionIfReleased((ReferenceCounted) object);
        } else {
            requireNonNull(object);
        }
    }

}