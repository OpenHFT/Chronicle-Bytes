package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import net.openhft.chronicle.core.threads.CleaningThread;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.openhft.chronicle.core.io.AbstractCloseable.waitForCloseablesToClose;
import static net.openhft.chronicle.core.io.AbstractReferenceCounted.assertReferencesReleased;

public class BytesTestCommon {

    private final Map<Predicate<ExceptionKey>, String> ignoredExceptions = new LinkedHashMap<>();
    private final Map<Predicate<ExceptionKey>, String> expectedExceptions = new LinkedHashMap<>();
    protected ThreadDump threadDump;
    protected Map<ExceptionKey, Integer> exceptions;

    public BytesTestCommon() {
        // Allocation of 0 chunk in D:\BuildAgent\work\9605994e6a194885\single-mapped-file21723892241386086929bin took 0.509 ms.
        ignoreException("Allocation of ");
    }

    @Before
    @BeforeEach
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    @Before
    @BeforeEach
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Before
    @BeforeEach
    public void recordExceptions() {
        exceptions = Jvm.recordExceptions();
    }

    public void ignoreException(String message) {
        ignoreException(k -> k.message.contains(message) || (k.throwable != null && k.throwable.getMessage().contains(message)), message);
    }

    public void ignoreException(Predicate<ExceptionKey> predicate, String description) {
        ignoredExceptions.put(predicate, description);
    }

    public void expectException(String message) {
        expectException(k -> k.message.contains(message) || (k.throwable != null && k.throwable.getMessage().contains(message)), message);
    }

    public void expectException(Predicate<ExceptionKey> predicate, String description) {
        expectedExceptions.put(predicate, description);
    }

    public void checkExceptions() {
        for (Map.Entry<Predicate<ExceptionKey>, String> expectedException : expectedExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(expectedException.getKey()))
                Slf4jExceptionHandler.WARN.on(getClass(), "No error for " + expectedException.getValue());
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

    @After
    @AfterEach
    public void afterChecks() {
        CleaningThread.performCleanup(Thread.currentThread());

        // find any discarded resources.
        System.gc();
        waitForCloseablesToClose(100);

        assertReferencesReleased();
        checkThreadDump();
        checkExceptions();
    }
}