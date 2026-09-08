/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.After;
import org.junit.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

// The outer test must not install its own BytesTestCommon exception recorder.
public class BytesTestCommonTest {

    private static Bytes<?> retainedBytes;

    @After
    public void cleanUpFixture() {
        try {
            final Bytes<?> bytes = retainedBytes;
            retainedBytes = null;
            if (bytes != null)
                bytes.releaseLast();
        } finally {
            Jvm.resetExceptionHandlers();
        }
    }

    @Test
    public void acceptsReleasedResourcesAndExpectedWarning() {
        TestExecutionSummary summary = runFixture("cleanTest");
        assertEquals(0, summary.getTestsFailedCount());
        assertEquals(1, summary.getTestsSucceededCount());
    }

    @Test
    public void rejectsMissingExpectedWarning() {
        assertFixtureFails("missingExpectedWarning", "No error for harness-expected-warning");
    }

    @Test
    public void rejectsUnexpectedWarning() {
        assertFixtureFails("unexpectedWarning", "harness-unexpected-warning");
    }

    @Test
    public void rejectsRetainedReference() {
        assertFixtureFails("retainedReference", "not released");
    }

    private static void assertFixtureFails(String method, String message) {
        TestExecutionSummary summary = runFixture(method);
        assertEquals(1, summary.getTestsFailedCount());
        assertEquals(0, summary.getTestsSucceededCount());
        assertEquals(1, summary.getFailures().size());
        Throwable failure = summary.getFailures().get(0).getException();
        assertTrue(failure.toString(), failure instanceof AssertionError);
        assertTrue(failure.toString(), failure.getMessage().contains(message));
    }

    private static TestExecutionSummary runFixture(String method) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectMethod(ChecksFixture.class, method))
                .filters(includeEngines("junit-jupiter"))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                .build(), listener);
        TestExecutionSummary summary = listener.getSummary();
        assertEquals(1, summary.getTestsFoundCount());
        assertEquals(1, summary.getTestsStartedCount());
        assertEquals(0, summary.getTestsSkippedCount());
        assertEquals(0, summary.getTestsAbortedCount());
        assertEquals(0, summary.getContainersFailedCount());
        return summary;
    }

    // Selected explicitly above; these deliberately faulty fixtures are not suite entry points.
    static class ChecksFixture extends BytesTestCommon {

        @org.junit.jupiter.api.Test
        void cleanTest() {
            Bytes<?> bytes = Bytes.allocateElasticDirect(16);
            try {
                expectException("harness-expected-warning");
                Jvm.warn().on(getClass(), "harness-expected-warning");
            } finally {
                bytes.releaseLast();
            }
        }

        @org.junit.jupiter.api.Test
        void missingExpectedWarning() {
            expectException("harness-expected-warning");
        }

        @org.junit.jupiter.api.Test
        void unexpectedWarning() {
            Jvm.warn().on(getClass(), "harness-unexpected-warning");
        }

        @org.junit.jupiter.api.Test
        void retainedReference() {
            // Keep the allocation alive until the owning Jupiter invocation has completed.
            retainedBytes = Bytes.allocateElasticDirect(16);
        }
    }
}
