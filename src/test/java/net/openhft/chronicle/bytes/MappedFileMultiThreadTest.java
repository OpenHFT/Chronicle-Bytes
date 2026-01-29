/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.util.Time;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests MappedFile multi-threaded lock behaviour because concurrent access
 * to memory-mapped files requires proper synchronisation to avoid data
 * races and memory corruption.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("Mapped file multi thread lock behaviour")
public class MappedFileMultiThreadTest extends BytesTestCommon {
    // Limit parallelism on WSL to reduce resource contention; use all cores on native
    private static final int DEFAULT_CORES = isWsl()
            // WSL: limit parallelism to reduce resource contention in emulated environment
            ? Math.min(2, Runtime.getRuntime().availableProcessors())
            // Native: query processor count for maximum concurrency stress testing
            : Runtime.getRuntime().availableProcessors();
    private static final int CORES = Integer.getInteger("cores", DEFAULT_CORES);
    private static final int DEFAULT_RUNTIME_MS = isWsl() ? 500 : 2_000;
    // System property allows custom runtime duration for extended testing
    private static final int RUNTIME_MS = Integer.getInteger("runtimems", DEFAULT_RUNTIME_MS);
    // System property allows test file path override for isolation
    private static final String TMP_FILE = System.getProperty("file", defaultTempFile());

    private static String defaultTempFile() {
        Path targetDir = Paths.get(OS.getTarget());
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new IORuntimeException("Unable to create target directory: " + targetDir, e);
        }
        return targetDir.resolve("testMultiThreadLock-" + Time.uniqueId() + ".tmp").toAbsolutePath().toString();
    }

    @SuppressWarnings("EmptyMethod")
    @BeforeEach
    public void threadDump() {
        super.threadDump();
    }

    @Test
    @DisplayName("multi thread mapped file lock stays stable")
    public void testMultiThreadLock() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for multithreaded mapped file test");
        assumeFalse(isWsl(),
                "WSL memory mapping can trigger unsafe access faults in this multi-threaded test");

        final List<String> garbage = Collections.synchronizedList(new ArrayList<>());
        final long chunkSize = (OS.isWindows() || isWsl()) ? 64 << 10 : 4 << 10;
        try (MappedFile mf = MappedFile.mappedFile(TMP_FILE, chunkSize, 0)) {
            assertEquals("refCount: 1", mf.referenceCounts(),
                    "Initial reference counts string matches expected value");

            final List<Future<?>> futures = new ArrayList<>();
            final ExecutorService es = Executors.newFixedThreadPool(CORES);
            final AtomicBoolean running = new AtomicBoolean(true);
            for (int i = 0; i < CORES; i++) {
                int finalI = i;
                futures.add(es.submit(() -> {
                    long offset = 1;
                    final ReferenceOwner test = ReferenceOwner.temporary("test" + finalI);
                    while (running.get()) {
                        garbage.add(test.referenceName() + offset);
                        MappedBytesStore bs = null;
                        Bytes<?> bytes = null;
                        try {
                            bs = mf.acquireByteStore(test, chunkSize * offset);
                            bytes = bs.bytesForRead();
                            assertNotNull(bytes.toString(),
                                    "Bytes toString returns non null at offset " + offset + " thread i " + finalI); // show it doesn't blow up.
                            assertNotNull(bs.toString(),
                                    "BytesStore toString returns non null at offset " + offset + " thread i " + finalI); // show it doesn't blow up.
                            ++offset;
                        } catch (IOException e) {
                            // Rethrow as unchecked to propagate within parallel stream
                            throw Jvm.rethrow(e);
                        } finally {
                            if (bytes != null) bytes.releaseLast();
                            if (bs != null) bs.release(test);
                        }
                        if (finalI == 0 && offset % 1_000 == 0) {
                            garbage.clear();
                            // Memory pressure test: trigger GC to validate finaliser behaviour
                            System.gc();
                        }
                    }
                }));
            }

            Jvm.pause(RUNTIME_MS);
            running.set(false);
            for (Future<?> f : futures)
                f.get(5, TimeUnit.SECONDS);
            es.shutdownNow();
            es.awaitTermination(1, TimeUnit.SECONDS);
        }
        Path tempFile = Paths.get(TMP_FILE);
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            throw new IORuntimeException("Unable to delete temp file: " + tempFile, e);
        }
    }
}
