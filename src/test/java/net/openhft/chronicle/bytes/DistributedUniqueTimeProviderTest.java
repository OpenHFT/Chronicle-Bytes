/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.time.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
@DisplayName("Distributed unique time provider monotonicity and concurrency checks")
public class DistributedUniqueTimeProviderTest extends BytesTestCommon {

    private DistributedUniqueTimeProvider timeProvider;
    private SetTimeProvider setTimeProvider;

    @BeforeEach
    public void setUp() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for unique time tests");

        timeProvider = DistributedUniqueTimeProvider.instance();
        setTimeProvider = new SetTimeProvider(SystemTimeProvider.INSTANCE.currentTimeNanos());
        timeProvider.provider(setTimeProvider);
    }

    private static volatile long blackHole;

    @BeforeAll
    public static void checks() throws IOException {
        System.setProperty("timestamp.dir", OS.getTarget());
        final File file = new File(BytesUtil.TIME_STAMP_PATH);
        deleteIfPossible(file);
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            assertNotNull(fos, "Timestamp file output stream is available");
        }
    }

    @Test
    @DisplayName("currentTimeMicros returns increasing microsecond values during loop")
    public void currentTimeMicros() {
        long last = 0;
        for (int i = 0; i < 100_000; i++) {
            long time = timeProvider.currentTimeMicros();
            assertTrue(time > last,
                    "Microsecond time increases on iteration " + i);
            assertEquals(LongTime.toMicros(time), time,
                    "Microsecond value stays aligned on iteration " + i);
            last = time;
        }
    }

    @Test
    @DisplayName("currentTimeMicros performance stays above baseline rate")
    public void currentTimeMicrosPerf() {
        long start = System.currentTimeMillis(), end;
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = timeProvider.currentTimeMicros();
            count += 1000;
        } while ((end = System.currentTimeMillis()) < start + 500);
        long rate = 1000L * count / (end - start);
        System.out.printf("currentTimeMicrosPerf count/sec: %,d%n", rate);
        assertTrue(count > 128_000 / 2,
                "Microsecond call count stays above baseline with count " + count);
        assertTrue(blackHole > 0,
                "Black hole value " + blackHole + " should be > 0 for microsecond timestamp");
    }

    @Test
    @DisplayName("currentTimeNanos performance stays above baseline rate")
    public void currentTimeNanosPerf() {
        long start = System.currentTimeMillis(), end;
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = timeProvider.currentTimeNanos();
            count += 1000;
        } while ((end = System.currentTimeMillis()) < start + 500);
        long rate = 1000L * count / (end - start);
        System.out.printf("currentTimeNanosPerf count/sec: %,d%n", rate);
        assertTrue(count > 202_000 / 2,
                "Nanosecond call count stays above baseline with count " + count);
        assertTrue(blackHole > 0,
                "Black hole value " + blackHole + " should be > 0 for nanosecond timestamp");
    }

    @Test
    @DisplayName("currentTimeNanos returns increasing nanos over duration")
    public void currentTimeNanos() {
        long start = timeProvider.currentTimeNanos();
        long last = start;
        int count = 0;
        long runTime = Jvm.isArm() ? 3_000_000_000L : 500_000_000L;
        for (; ; ) {
            long now = timeProvider.currentTimeNanos();
            assertEquals(LongTime.toNanos(now), now,
                    "Nanosecond value stays aligned after conversion");
            if (now > start + runTime)
                break;
            // check the times are different after shifting by 5 bits.
            assertTrue((now >>> 5) > (last >>> 5),
                    "Shifted time advances with now=" + now + " last=" + last);
            last = now;
            count++;
            if (count >= 10_000_000)
                break;
        }
        System.out.printf("count: %,d%n", count);
        assertTrue(count > 1_000_000,
                "Nanosecond loop exceeds minimum count " + count);
    }

    @Test
    @DisplayName("concurrentTimeNanos remains unique across threads during run")
    public void concurrentTimeNanos() {
        finishedNormally = false;
        long start0 = System.nanoTime();
        final int runTimeUS = 5_000_000;
        final int threads = Jvm.isArm() ? 4 : 16;
        final int stride = Jvm.isArm() ? 1 : threads;
        IntStream.range(0, threads)
                .parallel()
                .forEach(i -> {
                    try (DistributedUniqueTimeProvider tp = DistributedUniqueTimeProvider.forHostId(i)) {
                        long last = 0;
                        for (int j = 0; j < runTimeUS; j += stride) {
                            long now = tp.currentTimeNanos();
                            assertEquals(i, DistributedUniqueTimeProvider.hostIdFor(now),
                                    "Host id matches assigned value on step " + j);
                            assertTrue(now > last,
                                    "Thread time increases on step " + j);
                            last = now;
                        }
                    }
                });
        long time0 = System.nanoTime() - start0;
        System.out.printf("Time: %,d ms%n", time0 / 1_000_000);
        assertTrue(Jvm.isArm() || Jvm.isCodeCoverage()
                || time0 < runTimeUS * 1000L,
                "Concurrent time run stays within duration with time " + time0);
        finishedNormally = true;
    }

    @Test
    @DisplayName("timestampFor produces monotonic values for derived timestamps")
    public void testMonotonicallyIncreasing() {
        long last = 0;
        for (int i = 0; i < 10_000; i++) {
            long now = DistributedUniqueTimeProvider.timestampFor(timeProvider.currentTimeNanos());
            assertTrue(now > last,
                    "Timestamp advances on iteration " + i);
            last = now;
        }
    }

    @Test
    @DisplayName("unique micros across threads remain monotonic per iteration")
    public void shouldProvideUniqueTimeAcrossThreadsMicros() throws InterruptedException {
        final Set<Long> allGeneratedTimestamps = ConcurrentHashMap.newKeySet();
        final int numberOfThreads = 50;
        final int factor = 50;
        final int iterationsPerThread = 500;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads * factor);

        for (int i = 0; i < numberOfThreads * factor; i++) {
            executor.execute(() -> {
                try {
                    List<Long> threadTimeSet = new ArrayList<>(iterationsPerThread);
                    long lastTimestamp = 0;
                    for (int j = 0; j < iterationsPerThread; j++) {

                        // there could be a race condition for the next two methods, but it shouldn't matter for this test
                        setTimeProvider.advanceNanos(j);
                        long currentTimeMicros = timeProvider.currentTimeMicros();

                        threadTimeSet.add(currentTimeMicros);
                        assertTrue(currentTimeMicros > lastTimestamp,
                                "Microsecond timestamp increases on iteration " + j);
                        lastTimestamp = currentTimeMicros;
                    }
                    allGeneratedTimestamps.addAll(threadTimeSet);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(numberOfThreads * iterationsPerThread * factor, allGeneratedTimestamps.size(),
                "All microsecond timestamps across threads are unique");
    }

    @Test
    @DisplayName("unique nanos across threads remain monotonic per iteration")
    public void shouldProvideUniqueTimeAcrossThreadsNanos() throws InterruptedException {
        final Set<Long> allGeneratedTimestamps = ConcurrentHashMap.newKeySet();
        final int numberOfThreads = 50;
        final int factor = 50;
        final int iterationsPerThread = 500;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch latch = new CountDownLatch(numberOfThreads * factor);

        for (int i = 0; i < numberOfThreads * factor; i++) {
            executor.execute(() -> {
                try {
                    List<Long> threadTimeSet = new ArrayList<>(iterationsPerThread);
                    long lastTimestamp = 0;
                    for (int j = 0; j < iterationsPerThread; j++) {

                        // there could be a race condition for the next two methods, but it shouldn't matter for this test
                        setTimeProvider.advanceNanos(j);
                        long currentTimeNanos = timeProvider.currentTimeNanos();

                        threadTimeSet.add(currentTimeNanos);
                        assertTrue(currentTimeNanos > lastTimestamp,
                                "Nanosecond timestamp increases on iteration " + j);
                        lastTimestamp = currentTimeNanos;
                    }
                    allGeneratedTimestamps.addAll(threadTimeSet);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(numberOfThreads * iterationsPerThread * factor, allGeneratedTimestamps.size(),
                "All nanosecond timestamps across threads are unique");
    }

    @Test
    @DisplayName("deduplicator compares and retains per host id")
    public void deduplicatorComparesAndRetains() {
        BinaryLongArrayReference values = new BinaryLongArrayReference(DistributedUniqueTimeProvider.HOST_IDS);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(Math.toIntExact(values.maxSize()));
        try {
            BinaryLongArrayReference.write(bytes, DistributedUniqueTimeProvider.HOST_IDS);
            values.bytesStore(bytes, 0, values.maxSize());

            DistributedUniqueTimeDeduplicator deduplicator =
                    DistributedUniqueTimeProvider.newVanillaDeduplicator(values);
            int hostId = 7;
            long baseTime = 1_000L;
            long initial = baseTime + hostId;
            values.setValueAt(hostId, initial);

            assertEquals(0,
                    deduplicator.compareByHostId(initial),
                    "Deduplicator should report equality for identical timestamps");
            assertTrue(deduplicator.compareByHostId(initial + DistributedUniqueTimeProvider.HOST_IDS) > 0,
                    "Deduplicator should report newer timestamps as greater");
            assertTrue(deduplicator.compareByHostId(initial - DistributedUniqueTimeProvider.HOST_IDS) < 0,
                    "Deduplicator should report older timestamps as smaller");

            long newer = initial + DistributedUniqueTimeProvider.HOST_IDS;
            assertTrue(deduplicator.compareAndRetainNewer(newer) > 0,
                    "Deduplicator should retain newer timestamps");
            assertEquals(newer,
                    values.getValueAt(hostId),
                    "Deduplicator should update stored timestamp");

            long older = initial;
            assertTrue(deduplicator.compareAndRetainNewer(older) < 0,
                    "Deduplicator should ignore older timestamps");
            assertEquals(newer,
                    values.getValueAt(hostId),
                    "Deduplicator should keep the newer timestamp");
        } finally {
            values.close();
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("call rate advance keeps micros monotonic")
    public void shouldAdvanceTimeWhenExceedingCallsPerSecond() {
        final int iterations = 1_000_001;
        long lastTimeMicros = 0;

        for (int i = 0; i < iterations; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeMicros = timeProvider.currentTimeMicros();
            assertTrue(currentTimeMicros > lastTimeMicros,
                    "Microsecond timestamps increase on iteration " + i);
            lastTimeMicros = currentTimeMicros;
        }
    }

    @Test
    @DisplayName("currentTimeMillis stays within expected range for iterations")
    public void currentTimeMillisShouldBeCorrect() {
        int iterations = 1_000;
        long lastTimeMillis = 0;
        final long startTimeMillis = setTimeProvider.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeMillis = timeProvider.currentTimeMillis();
            assertTrue(currentTimeMillis >= startTimeMillis,
                    "Millis stay above start on iteration " + i);
            assertTrue(currentTimeMillis <= startTimeMillis + iterations,
                    "Millis stay below bound on iteration " + i);
            assertTrue(currentTimeMillis >= lastTimeMillis,
                    "Millisecond timestamps increase or stay on iteration " + i);
            lastTimeMillis = currentTimeMillis;
        }
    }

    @Test
    @DisplayName("currentTimeMicros stays monotonic for forward steps")
    public void currentTimeMicrosShouldBeCorrect() {
        long lastTimeMicros = 0;

        for (int i = 0; i < 4_000; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeMicros = timeProvider.currentTimeMicros();
            assertTrue(currentTimeMicros > lastTimeMicros,
                    "Microsecond timestamps increase on forward step " + i);
            lastTimeMicros = currentTimeMicros;
        }
    }

    @Test
    @DisplayName("currentTimeMicros stays monotonic for backward steps")
    public void currentTimeMicrosShouldBeCorrectBackwards() {
        long lastTimeMicros = 0;

        for (int i = 0; i < 4_000; i++) {
            setTimeProvider.advanceNanos(-i);
            long currentTimeMicros = timeProvider.currentTimeMicros();
            assertTrue(currentTimeMicros > lastTimeMicros,
                    "Microsecond timestamps increase on backward step " + i);
            lastTimeMicros = currentTimeMicros;
        }
    }

    @Test
    @DisplayName("currentTimeNanos stays monotonic for forward steps")
    public void currentTimeNanosShouldBeCorrect() {
        long lastTimeNanos = 0;

        for (int i = 0; i < 4_000; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeNanos = timeProvider.currentTimeNanos();
            assertTrue(currentTimeNanos > lastTimeNanos,
                    "Nanosecond timestamps increase on iteration " + i);
            lastTimeNanos = currentTimeNanos / 1000;
        }
    }
}
