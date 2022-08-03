/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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