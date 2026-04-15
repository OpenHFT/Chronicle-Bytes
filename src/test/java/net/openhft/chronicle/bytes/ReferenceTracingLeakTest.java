/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceTracingLeakTest extends BytesTestCommon {

    @Test
    public void leakDetectionReportsCreatedHere() throws Exception {
        final NativeBytes<Void> leaked = Bytes.allocateElasticDirect(64);
        try {
            assertNotNull(((AbstractReferenceCounted) leaked).createdHere(),
                    "createdHere should be recorded for traced resources");

            final AssertionError leak = assertThrows(AssertionError.class, AbstractReferenceCounted::assertReferencesReleased);
            assertTrue(leak.getSuppressed().length > 0, "Expect suppressed entries describing the leak");
        } finally {
            leaked.releaseLast();
        }
        AbstractReferenceCounted.assertReferencesReleased();
    }
}
