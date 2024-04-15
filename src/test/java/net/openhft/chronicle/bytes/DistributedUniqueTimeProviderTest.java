/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.time.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DistributedUniqueTimeProviderTest extends BytesTestCommon {

    private DistributedUniqueTimeProvider timeProvider;
    private SetTimeProvider setTimeProvider;

    @Before
    public void setUp() {
        timeProvider = DistributedUniqueTimeProvider.instance();
        setTimeProvider = new SetTimeProvider(SystemTimeProvider.INSTANCE.currentTimeNanos());
        timeProvider.provider(setTimeProvider);
    }

    static volatile long blackHole;

    @BeforeClass
    public static void checks() throws IOException {
        System.setProperty("timestamp.dir", OS.getTarget());
        final File file = new File(BytesUtil.TIME_STAMP_PATH);
        deleteIfPossible(file);
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
        }
    }

    @Test
    public void currentTimeMicros() {
        long last = 0;
        for (int i = 0; i < 100_000; i++) {
            long time = timeProvider.currentTimeMicros();
            assertTrue(time > last);
            assertEquals(LongTime.toMicros(time), time);
            last = time;
        }
    }

    @Test
    public void currentTimeMicrosPerf() {
        long start = System.currentTimeMillis(), end;
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = ((TimeProvider) timeProvider).currentTimeMicros();
            count += 1000;
        } while ((end = System.currentTimeMillis()) < start + 500);
        long rate = 1000L * count / (end - start);
        System.out.printf("currentTimeMicrosPerf count/sec: %,d%n", rate);
        assertTrue(count > 128_000 / 2); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanosPerf() {
        long start = System.currentTimeMillis(), end;
        int count = 0;
        do {
            for (int i = 0; i < 1000; i++)
                blackHole = ((TimeProvider) timeProvider).currentTimeNanos();
            count += 1000;
        } while ((end = System.currentTimeMillis()) < start + 500);
        long rate = 1000L * count / (end - start);
        System.out.printf("currentTimeNanosPerf count/sec: %,d%n", rate);
        assertTrue(count > 202_000 / 2); // half the speed of Rasberry Pi
    }

    @Test
    public void currentTimeNanos() {
        long start = ((TimeProvider) timeProvider).currentTimeNanos();
        long last = start;
        int count = 0;
        long runTime = Jvm.isArm() ? 3_000_000_000L : 500_000_000L;
        for (; ; ) {
            long now = ((TimeProvider) timeProvider).currentTimeNanos();
            assertEquals(LongTime.toNanos(now), now);
            if (now > start + runTime)
                break;
            // check the times are different after shifting by 5 bits.
            assertTrue((now >>> 5) > (last >>> 5));
            last = now;
            count++;
            if (count >= 10_000_000)
                break;
        }
        System.out.printf("count: %,d%n", count);
        assertTrue(count > 1_000_000);
    }

    @Test
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
                            assertEquals(i, DistributedUniqueTimeProvider.hostIdFor(now));
                            assertTrue(now > last);
                            last = now;
                        }
                    }
                });
        long time0 = System.nanoTime() - start0;
        System.out.printf("Time: %,d ms%n", time0 / 1_000_000);
        assertTrue(Jvm.isArm() || Jvm.isCodeCoverage()
                || time0 < runTimeUS * 1000L);
        finishedNormally = true;
    }

    @Test
    public void testMonotonicallyIncreasing() {
        long last = 0;
        for (int i = 0; i < 10_000; i++) {
            long now = DistributedUniqueTimeProvider.timestampFor(((TimeProvider) timeProvider).currentTimeNanos());
            assertTrue(now > last);
            last = now;
        }
    }

    @Test
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
                        assertTrue("Timestamps should always increase", currentTimeMicros > lastTimestamp);
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

        assertEquals("All timestamps across all threads and iterations should be unique",
                numberOfThreads * iterationsPerThread * factor, allGeneratedTimestamps.size());
    }

    @Test
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
                        assertTrue("Timestamps should always be increasing", currentTimeNanos > lastTimestamp);
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

        assertEquals("All timestamps across all threads and iterations should be unique",
                numberOfThreads * iterationsPerThread * factor, allGeneratedTimestamps.size());
    }

    @Test
    public void shouldAdvanceTimeWhenExceedingCallsPerSecond() {
        final int iterations = 1_000_001;
        long lastTimeMicros = 0;

        for (int i = 0; i < iterations; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeMicros = timeProvider.currentTimeMicros();
            assertTrue("Each timestamp must be greater than the last", currentTimeMicros > lastTimeMicros);
            lastTimeMicros = currentTimeMicros;
        }
    }

    @Test
    public void currentTimeMillisShouldBeCorrect() {
        int iterations = 1_000;
        long lastTimeMillis = 0;
        final long startTimeMillis = setTimeProvider.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeMillis = timeProvider.currentTimeMillis();
            assertTrue(currentTimeMillis >= startTimeMillis);
            assertTrue(currentTimeMillis <= startTimeMillis + iterations);
            assertTrue("Millisecond timestamps must increase or be the same", currentTimeMillis >= lastTimeMillis);
            lastTimeMillis = currentTimeMillis;
        }
    }

    @Test
    public void currentTimeMicrosShouldBeCorrect() {
        long lastTimeMicros = 0;

        for (int i = 0; i < 4_000; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeMicros = timeProvider.currentTimeMicros();
            assertTrue("Microsecond timestamps must increase", currentTimeMicros > lastTimeMicros);
            lastTimeMicros = currentTimeMicros;
        }
    }

    @Test
    public void currentTimeMicrosShouldBeCorrectBackwards() {
        long lastTimeMicros = 0;

        for (int i = 0; i < 4_000; i++) {
            setTimeProvider.advanceNanos(-i);
            long currentTimeMicros = timeProvider.currentTimeMicros();
            assertTrue("Microsecond timestamps must increase", currentTimeMicros > lastTimeMicros);
            lastTimeMicros = currentTimeMicros;
        }
    }

    @Test
    public void currentTimeNanosShouldBeCorrect() {
        long lastTimeNanos = 0;

        for (int i = 0; i < 4_000; i++) {
            setTimeProvider.advanceNanos(i);
            long currentTimeNanos = timeProvider.currentTimeNanos();
            assertTrue("Nanosecond timestamps should increase", currentTimeNanos > lastTimeNanos);
            lastTimeNanos = currentTimeNanos / 1000;
        }
    }
}
