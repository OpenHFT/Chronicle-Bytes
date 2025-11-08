/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReferenceTracingLeakTest extends BytesTestCommon {

    @Test
    public void leakDetectionReportsCreatedHere() throws Exception {
        final NativeBytes<Void> leaked = Bytes.allocateElasticDirect(64);
        try {
            assertNotNull("createdHere should be recorded for traced resources",
                    ((AbstractReferenceCounted) leaked).createdHere());

            final AssertionError leak = assertThrows(AssertionError.class, AbstractReferenceCounted::assertReferencesReleased);
            assertTrue("Expect suppressed entries describing the leak", leak.getSuppressed().length > 0);
        } finally {
            leaked.releaseLast();
        }
        AbstractReferenceCounted.assertReferencesReleased();
    }
}
