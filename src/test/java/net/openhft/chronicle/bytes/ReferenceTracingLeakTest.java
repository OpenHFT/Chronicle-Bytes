/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests reference tracing leak detection because allocation site tracking
 * is essential to diagnose unreleased off-heap resources during
 * development.
 */
@DisplayName("Reference tracing leak detection and allocation tracking")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class ReferenceTracingLeakTest extends BytesTestCommon {

    @Test
    @DisplayName("leak detection reports allocation site for traced bytes")
    public void leakDetectionReportsCreatedHere() {
        final NativeBytes<Void> leaked = Bytes.allocateElasticDirect(64);
        try {
            assertNotNull(leaked.createdHere(),
                    "createdHere should be recorded for traced resources");

            final AssertionError leak = assertThrows(AssertionError.class,
                    AbstractReferenceCounted::assertReferencesReleased,
                    "Leak detection should throw when references remain");
            int suppressed = leak.getSuppressed().length;
            assertTrue(suppressed > 0,
                    "Expect suppressed entries describing the leak, but was " + suppressed);
        } finally {
            leaked.releaseLast();
        }
        AbstractReferenceCounted.assertReferencesReleased();
    }
}
