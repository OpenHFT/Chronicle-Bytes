//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DecoratedBufferOverflowExceptionTest {

    @Test
    public void nullCauseIsEquivalentToNoCause() {
        DecoratedBufferOverflowException withNull =
                new DecoratedBufferOverflowException("with-null", null);
        DecoratedBufferOverflowException withoutCause =
                new DecoratedBufferOverflowException("without-cause");

        assertNull(withNull.getCause());
        assertNull(withoutCause.getCause());
    }

    @Test
    public void nonNullCauseIsAttached() {
        Throwable cause = new IllegalStateException("boom");
        DecoratedBufferOverflowException exception =
                new DecoratedBufferOverflowException("with-cause", cause);
        assertSame(cause, exception.getCause());
    }
}
