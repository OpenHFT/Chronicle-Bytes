/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import net.openhft.chronicle.core.threads.CleaningThread;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.openhft.chronicle.core.io.AbstractCloseable.waitForCloseablesToClose;
import static net.openhft.chronicle.core.io.AbstractReferenceCounted.assertReferencesReleased;

/**
 * Common base class for Chronicle Bytes tests providing exception handling, thread dump checking,
 * and resource cleanup because consistent test infrastructure is essential for reliable test execution.
 */
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate") // Base class is used by tests in subpackages.
public class BytesTestCommon {
    // WSL detection uses System.getenv because these environment variables are set by WSL itself
    private static final boolean IS_WSL =
            // System.getenv is required to detect WSL runtime environment
            System.getenv("WSL_DISTRO_NAME") != null
                    // System.getenv is required to detect WSL interop support
                    || System.getenv("WSL_INTEROP") != null
                    // System.getenv is required to detect WSL environment variables
                    || System.getenv("WSLENV") != null;

    protected static boolean isWsl() {
        return IS_WSL;
    }

    private final Map<Predicate<ExceptionKey>, String> ignoredExceptions = new LinkedHashMap<>();
    private final Map<Predicate<ExceptionKey>, String> expectedExceptions = new LinkedHashMap<>();
    private ThreadDump threadDump;
    private Map<ExceptionKey, Integer> exceptions;
    boolean finishedNormally;

    @SuppressWarnings("this-escape")
    protected BytesTestCommon() {
        // Allocation of 0 chunk in D:\BuildAgent\work\9605994e6a194885\single-mapped-file21723892241386086929bin took 0.509 ms.
        ignoreException("Allocation of ");
    }

    private static boolean contains(String text, String message) {
        return text != null && text.contains(message);
    }

    @BeforeEach
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    public void threadDump() {
        threadDump = new ThreadDump();
    }

    private void checkThreadDump() {
        if (threadDump != null)
            threadDump.assertNoNewThreads();
    }

    @BeforeEach
    public void recordExceptions() {
        exceptions = Jvm.recordExceptions();
    }

    @BeforeEach
    public void assumeFinishedNormally() {
        finishedNormally = true;
    }

    void ignoreException(String message) {
        ignoreException(k -> contains(k.message, message) || (k.throwable != null && contains(k.throwable.getMessage(), message)), message);
    }

    protected void expectException(String message) {
        expectException(k -> contains(k.message, message) || (k.throwable != null && contains(k.throwable.getMessage(), message)), message);
    }

    private void ignoreException(Predicate<ExceptionKey> predicate, String description) {
        ignoredExceptions.put(predicate, description);
    }

    void expectException(Predicate<ExceptionKey> predicate, String description) {
        expectedExceptions.put(predicate, description);
    }

    private void checkExceptions() {
        for (Map.Entry<Predicate<ExceptionKey>, String> expectedException : expectedExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(expectedException.getKey()))
                throw new AssertionError("No error for " + expectedException.getValue());
        }
        expectedExceptions.clear();

        for (Map.Entry<Predicate<ExceptionKey>, String> ignoredException : ignoredExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(ignoredException.getKey()))
                Slf4jExceptionHandler.DEBUG.on(getClass(), "Ignored " + ignoredException.getValue());
        }
        ignoredExceptions.clear();

        if (Jvm.hasException(exceptions)) {
            final String msg = exceptions.size() + " exceptions were detected: " + exceptions.keySet().stream().map(ek -> ek.message).collect(Collectors.joining(", "));
            Jvm.dumpException(exceptions);
            Jvm.resetExceptionHandlers();
            throw new AssertionError(msg);
        }
    }

    @AfterEach
    public void afterChecks() {
        cleanResources();

        if (finishedNormally) {
            assertReferencesReleased();
            checkThreadDump();
            checkExceptions();
        }
    }

    private static void cleanResources() {
        CleaningThread.performCleanup(Thread.currentThread());

        waitForCloseablesToClose(100);
        BackgroundResourceReleaser.releasePendingResources();
    }

    static void deleteIfPossible(@NotNull final File file) {
        if (!file.exists() || file.delete()) {
            return;
        }
        cleanResources();

        if (!file.exists() || file.delete()) {
            return;
        }
        Jvm.error().on(MappedMemoryTest.class, "Unable to delete " + file.getAbsolutePath());
    }
}
